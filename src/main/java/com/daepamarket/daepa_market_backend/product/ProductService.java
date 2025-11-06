package com.daepamarket.daepa_market_backend.product;

import com.daepamarket.daepa_market_backend.S3Service;
import com.daepamarket.daepa_market_backend.alarm.AlarmService;
import com.daepamarket.daepa_market_backend.domain.Category.CtLowEntity;
import com.daepamarket.daepa_market_backend.domain.Category.CtLowRepository;
import com.daepamarket.daepa_market_backend.domain.Category.CtMiddleEntity;
import com.daepamarket.daepa_market_backend.domain.deal.DealEntity;
import com.daepamarket.daepa_market_backend.domain.deal.DealRepository;
import com.daepamarket.daepa_market_backend.domain.product.ProductEntity;
import com.daepamarket.daepa_market_backend.domain.product.ProductRepository;
import com.daepamarket.daepa_market_backend.domain.productimage.ProductImageEntity;
import com.daepamarket.daepa_market_backend.domain.productimage.ProductImageRepository;
import com.daepamarket.daepa_market_backend.domain.user.UserEntity;
import com.daepamarket.daepa_market_backend.domain.user.UserRepository;
import com.daepamarket.daepa_market_backend.jwt.JwtProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductService {

    // 기본 리포지토리들
    private final ProductRepository productRepo;
    private final ProductImageRepository imageRepo;
    private final DealRepository dealRepo;
    private final UserRepository userRepo;
    private final CtLowRepository ctLowRepo;

    // 기타 서비스
    private final S3Service s3Service;
    private final AlarmService alarmService;
    private final JwtProvider jwtProvider;

    // =========================================================
    // 등록 (멀티파트)
    // =========================================================
    @Transactional
    public Long registerMultipart(Long userIdx, ProductCreateDTO dto, List<MultipartFile> images) {
        // 파일이 없으면 그냥 기본 등록
        if (images == null || images.isEmpty()) {
            dto.setImageUrls(List.of());
            return register(userIdx, dto);
        }

        // 파일 있으면 S3 올리고 url을 dto에 채워준다
        List<String> urls = images.stream()
                .map(file -> {
                    try {
                        return s3Service.uploadFile(file, "products");
                    } catch (IOException e) {
                        throw new RuntimeException("S3 업로드 중 오류 발생: " + file.getOriginalFilename(), e);
                    }
                })
                .toList();

        dto.setImageUrls(urls);
        return register(userIdx, dto);
    }

    // =========================================================
    // 등록
    // =========================================================
    @Transactional
    public Long register(Long userIdx, ProductCreateDTO dto) {

        // 판매자
        UserEntity seller = userRepo.findById(userIdx)
                .orElseThrow(() -> new IllegalArgumentException("판매자를 찾을 수 없습니다."));

        // 카테고리 검증
        CtLowEntity low = ctLowRepo.findById(dto.getLowId())
                .orElseThrow(() -> new IllegalArgumentException("하위 카테고리를 찾을 수 없습니다."));
        CtMiddleEntity middle = low.getMiddle();
        if (middle == null || !middle.getMiddleIdx().equals(dto.getMiddleId())) {
            throw new IllegalArgumentException("중위 카테고리가 하위와 일치하지 않습니다.");
        }
        if (middle.getUpper() == null || !middle.getUpper().getUpperIdx().equals(dto.getUpperId())) {
            throw new IllegalArgumentException("상위 카테고리가 중위와 일치하지 않습니다.");
        }

        // 상품 저장
        ProductEntity product = productRepo.save(
                ProductEntity.builder()
                        .seller(seller)
                        .ctLow(low)
                        .pdTitle(dto.getTitle())
                        .pdPrice(dto.getPrice())
                        .pdContent(dto.getContent())
                        .pdLocation(dto.getLocation())
                        .pdStatus(dto.getPdStatus()) // 0=중고, 1=새상품
                        .pdThumb(dto.getImageUrls() != null && !dto.getImageUrls().isEmpty()
                                ? dto.getImageUrls().get(0)
                                : null)
                        .pdHit(0)
                        .pdRef(0)
                        .pdCreate(LocalDateTime.now())
                        .pdUpdate(LocalDateTime.now())
                        .pdDel(false)
                        .pdRefdate(LocalDateTime.now())
                        .build()
        );

        // 이미지 저장
        List<String> urls = dto.getImageUrls();
        if (urls != null && !urls.isEmpty()) {
            urls.stream()
                    .limit(10)
                    .forEach(url -> imageRepo.save(
                            ProductImageEntity.builder()
                                    .product(product)
                                    .imageUrl(url)
                                    .createdAt(LocalDateTime.now())
                                    .updatedAt(LocalDateTime.now())
                                    .build()
                    ));
        }

        // 알람 매칭
        alarmService.createAlarmsForMatchingProduct(product);

        // 거래 기본값(등록할 때 하나 만들어두는 구조)
        DealEntity deal = DealEntity.builder()
                .product(product)
                .seller(seller)
                .buyer(null)
                .dDeal(dto.getDDeal()) // 프론트 dto는 대문자지만 DB는 컬럼 d_deal
                .dStatus(0L)
                .dSell(0L)             // ✅ 등록 시 기본값 0
                .build();
        dealRepo.save(deal);

        return product.getPdIdx();
    }

    // =========================================================
    // ✅ 수정 (이미지 포함)
    // =========================================================
    @Transactional
    public void updateMultipart(Long pdIdx, Long userIdx, ProductCreateDTO dto, List<MultipartFile> images) {

        ProductEntity product = getOwnedProduct(pdIdx, userIdx);

        // 프론트에서 유지한다고 보낸 기존 이미지 URL
        List<String> finalImageUrls = new ArrayList<>();
        if (dto.getImageUrls() != null) {
            finalImageUrls.addAll(dto.getImageUrls());
        }

        // 새로 올라온 파일이 있으면 S3에 올려서 뒤에 붙인다
        if (images != null && !images.isEmpty()) {
            for (MultipartFile file : images) {
                if (file.isEmpty()) continue;
                try {
                    String url = s3Service.uploadFile(file, "products");
                    finalImageUrls.add(url);
                } catch (IOException e) {
                    throw new RuntimeException("이미지 업로드 실패: " + file.getOriginalFilename(), e);
                }
            }
        }

        // 공통 수정
        updateProductInternal(product, dto, finalImageUrls);
    }

    // =========================================================
    // 수정 (이미지 안 바꾸는 경우)
    // =========================================================
    @Transactional
    public void updateProduct(Long pdIdx, Long userIdx, ProductCreateDTO dto) {
        ProductEntity product = getOwnedProduct(pdIdx, userIdx);

        // DB에 있는 현재 이미지들 그대로 가져온다
        List<String> currentImageUrls = imageRepo.findAllByProduct_PdIdx(pdIdx)
                .stream()
                .map(ProductImageEntity::getImageUrl)
                .toList();

        updateProductInternal(product, dto, new ArrayList<>(currentImageUrls));
    }

    // =========================================================
    // 실제 수정 내부 로직
    // =========================================================
    private void updateProductInternal(ProductEntity product, ProductCreateDTO dto, List<String> finalImageUrls) {

        // 카테고리 검증
        CtLowEntity low = ctLowRepo.findById(dto.getLowId())
                .orElseThrow(() -> new IllegalArgumentException("하위 카테고리를 찾을 수 없습니다."));
        CtMiddleEntity middle = low.getMiddle();
        if (middle == null || !middle.getMiddleIdx().equals(dto.getMiddleId())) {
            throw new IllegalArgumentException("중위 카테고리가 하위와 일치하지 않습니다.");
        }
        if (middle.getUpper() == null || !middle.getUpper().getUpperIdx().equals(dto.getUpperId())) {
            throw new IllegalArgumentException("상위 카테고리가 중위와 일치하지 않습니다.");
        }

        // 기본 필드 업데이트
        product.setCtLow(low);
        product.setPdTitle(dto.getTitle());
        product.setPdContent(dto.getContent());
        product.setPdPrice(dto.getPrice());
        product.setPdLocation(dto.getLocation());
        product.setPdStatus(dto.getPdStatus());
        product.setPdUpdate(LocalDateTime.now());

        // 대표 이미지
        if (!finalImageUrls.isEmpty()) {
            product.setPdThumb(finalImageUrls.get(0));
        } else {
            product.setPdThumb(null);
        }

        // 이미지 테이블 동기화
        List<ProductImageEntity> currentImages = imageRepo.findAllByProduct_PdIdx(product.getPdIdx());

        // 1) 현재 DB에 있는데 프론트에서 안 보낸 건 삭제
        for (ProductImageEntity img : currentImages) {
            if (!finalImageUrls.contains(img.getImageUrl())) {
                imageRepo.delete(img);
            }
        }

        // 2) 프론트에서 보냈는데 DB에 없는 건 추가
        for (String url : finalImageUrls) {
            boolean exists = currentImages.stream()
                    .anyMatch(ci -> ci.getImageUrl().equals(url));
            if (!exists) {
                imageRepo.save(
                        ProductImageEntity.builder()
                                .product(product)
                                .imageUrl(url)
                                .createdAt(LocalDateTime.now())
                                .updatedAt(LocalDateTime.now())
                                .build()
                );
            }
        }

        productRepo.save(product);

        // ✅ 거래방식도 같이 반영 (deal 테이블의 d_deal)
        dealRepo.findByProduct_PdIdx(product.getPdIdx()).ifPresent(deal -> {
            deal.setDDeal(dto.getDDeal());
            dealRepo.save(deal);
        });
    }

    // =========================================================
    // 목록 조회 (id 기준)
    // =========================================================
    @Transactional(readOnly = true)
    public Page<ProductEntity> getProductsByIds(
            Long upperId, Long middleId, Long lowId,
            String sort, int page, int size
    ) {
        Pageable pageable = PageRequest.of(page, size, resolveSort(sort));
        return productRepo.findAllByCategoryIds(upperId, middleId, lowId, pageable);
    }

    // =========================================================
    // 목록 조회 (이름 기준)
    // =========================================================
    @Transactional(readOnly = true)
    public Page<ProductEntity> getProductsByNames(
            String big, String mid, String sub,
            String sort, int page, int size
    ) {
        Pageable pageable = PageRequest.of(page, size, resolveSort(sort));
        return productRepo.findAllByNames(big, mid, sub, pageable);
    }

    // =========================================================
    // 마이페이지 내 상품 목록
    // =========================================================
    public List<productMyPageDTO> getMyProductByUIdx(Long uIdx, Integer status) {
        UserEntity user = userRepo.findById(uIdx)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "해당 회원이 없습니다. u_idx=" + uIdx));

        List<ProductEntity> products;
        if (status != null && (status == 0 || status == 1)) {
            products = productRepo.findBySellerAndPdStatus(user, status);
        } else {
            products = productRepo.findBySeller(user);
        }

        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        return products.stream()
                .map(p -> {
                    productMyPageDTO dto = new productMyPageDTO();
                    dto.setU_idx(user.getUIdx());
                    dto.setPd_status(String.valueOf(p.getPdStatus()));
                    dto.setPd_title(p.getPdTitle());
                    dto.setPd_price(p.getPdPrice() != null ? p.getPdPrice().intValue() : 0);
                    dto.setPd_create(p.getPdCreate() != null ? p.getPdCreate().format(fmt) : null);
                    dto.setPd_thumb(p.getPdThumb());
                    return dto;
                })
                .toList();
    }

    // =========================================================
    // 단건 상세
    // =========================================================
    @Transactional(readOnly = true)
    public ProductDetailDTO getProductDetail(Long pdIdx) {

        ProductEntity product = productRepo.findById(pdIdx)
                .orElseThrow(() -> new IllegalArgumentException("상품을 찾을 수 없습니다."));

        if (product.isPdDel()) {
            throw new ResponseStatusException(HttpStatus.GONE, "삭제된 상품입니다.");
        }

        UserEntity seller = product.getSeller();

        CtLowEntity low = product.getCtLow();
        CtMiddleEntity middle = low != null ? low.getMiddle() : null;

        String upperName = middle != null && middle.getUpper() != null
                ? middle.getUpper().getUpperCt()
                : null;
        String middleName = middle != null ? middle.getMiddleCt() : null;
        String lowName = low != null ? low.getLowCt() : null;

        List<String> imageUrls = imageRepo.findAllByProduct_PdIdx(pdIdx)
                .stream()
                .map(ProductImageEntity::getImageUrl)
                .toList();

        // ✅ deal 도 같이 조회해서 소문자 필드로 채워서 넘긴다
        DealEntity deal = dealRepo.findByProduct_PdIdx(pdIdx).orElse(null);

        Double sellerManner = seller != null ? seller.getUManner() : null;

        return ProductDetailDTO.builder()
                .pdIdx(product.getPdIdx())
                .pdTitle(product.getPdTitle())
                .pdPrice(product.getPdPrice())
                .pdContent(product.getPdContent())
                .pdLocation(product.getPdLocation())
                .location(product.getPdLocation())
                .pdStatus(product.getPdStatus())
                .pdThumb(product.getPdThumb())
                .images(imageUrls)
                .sellerId(seller != null ? seller.getUIdx() : null)
                .sellerName(seller != null ? seller.getUnickname() : null)
                .sellerAvatar(seller != null ? seller.getUProfile() : null)
                .sellerManner(sellerManner)
                .upperName(upperName)
                .middleName(middleName)
                .lowName(lowName)
                .upperId(middle != null && middle.getUpper() != null ? middle.getUpper().getUpperIdx() : null)
                .middleId(middle != null ? middle.getMiddleIdx() : null)
                .lowId(low != null ? low.getLowIdx() : null)
                .pdCreate(product.getPdCreate() != null ? product.getPdCreate().toString() : null)
                // 👇 여기 세 줄이 포인트
                .ddeal(deal != null ? deal.getDDeal() : null)
                .dsell(deal != null ? deal.getDSell() : null)
                .dstatus(deal != null ? deal.getDStatus() : null)
                .build();
    }

    // =========================================================
    // 연관 상품
    // =========================================================
    @Transactional(readOnly = true)
    public List<ProductEntity> getRelatedProducts(Long pdIdx, int limit) {
        ProductEntity base = productRepo.findById(pdIdx)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "상품을 찾을 수 없습니다."));

        if (base.isPdDel()) {
            return List.of();
        }

        Long lowId = base.getCtLow() != null ? base.getCtLow().getLowIdx() : null;

        return productRepo.findRelatedByLowIdExcludingSelf(
                lowId,
                pdIdx,
                PageRequest.of(0, limit)
        ).getContent();
    }

    // =========================================================
    // 소프트 삭제
    // =========================================================
    @Transactional
    public void softDeleteProduct(Long pdIdx, Long userIdx) {
        ProductEntity product = getOwnedProduct(pdIdx, userIdx);
        product.setPdDel(true);
        productRepo.save(product);
    }

    // =========================================================
    // 끌어올리기
    // =========================================================
    @Transactional
    public void bumpProduct(Long pdIdx, Long userIdx) {
        ProductEntity product = getOwnedProduct(pdIdx, userIdx);
        product.setPdRefdate(LocalDateTime.now());
        productRepo.save(product);
    }

    // =========================================================
    // 판매완료
    // =========================================================
    @Transactional
    public void completeProduct(Long pdIdx, Long userIdx) {
        // 본인 상품인지 확인
        getOwnedProduct(pdIdx, userIdx);

        // 상품 자체의 pdStatus(중고/새상품)는 건드리지 않고 deal 만 판매완료로
        dealRepo.findByProduct_PdIdx(pdIdx).ifPresent(deal -> {
            deal.setDSell(1L);
            deal.setDStatus(1L);
            dealRepo.save(deal);
        });
    }

    // =========================================================
    // 공통: 내 상품인지 확인
    // =========================================================
    private ProductEntity getOwnedProduct(Long pdIdx, Long userIdx) {
        ProductEntity product = productRepo.findById(pdIdx)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "상품을 찾을 수 없습니다."));
        if (product.getSeller() == null || !product.getSeller().getUIdx().equals(userIdx)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "본인 상품만 처리할 수 있습니다.");
        }
        return product;
    }

    // =========================================================
    // 정렬 기준
    // =========================================================
    private Sort resolveSort(String sort) {
        String key = (sort == null || sort.isBlank()) ? "recent" : sort;
        return switch (key) {
            case "price_asc"  -> Sort.by(Sort.Direction.ASC, "pdPrice");
            case "price_desc" -> Sort.by(Sort.Direction.DESC, "pdPrice");
            default -> Sort.by(Sort.Direction.DESC, "pdRefdate")
                    .and(Sort.by(Sort.Direction.DESC, "pdCreate"));
        };
    }
}
