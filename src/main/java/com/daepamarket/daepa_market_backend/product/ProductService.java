// src/main/java/com/daepamarket/daepa_market_backend/product/ProductService.java
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
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepo;
    private final ProductImageRepository imageRepo;
    private final DealRepository dealRepo;

    private final UserRepository userRepo;
    private final CtLowRepository ctLowRepo;

    private final S3Service s3Service;
    private final AlarmService alarmService;

    // 마이페이지 쪽에서 쓰던 것들
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final JwtProvider jwtProvider;

    // =========================================================
    // 등록 (멀티파트)
    // =========================================================
    @Transactional
    public Long registerMultipart(Long userIdx, ProductCreateDTO dto, List<MultipartFile> images) {
        if (images == null || images.isEmpty()) {
            dto.setImageUrls(List.of());
            return register(userIdx, dto);
        }

        List<String> urls = images.stream()
                .map(file -> {
                    try {
                        String folder = "products";
                        return s3Service.uploadFile(file, folder);
                    } catch (IOException e) {
                        throw new RuntimeException("S3 업로드 중 오류 발생: " + file.getOriginalFilename(), e);
                    }
                })
                .toList();

        dto.setImageUrls(urls);
        return register(userIdx, dto);
    }

    // =========================================================
    // 수정 (멀티파트) ← 새로 추가
    // 이미지 바꾸면 기존 이미지 싹 지우고 새로 넣어준다
    // =========================================================
    @Transactional
    public void updateMultipart(Long pdIdx, Long userIdx, ProductCreateDTO dto, List<MultipartFile> images) {
        // 먼저 내 소유 상품인지 체크
        ProductEntity product = getOwnedProduct(pdIdx, userIdx);

        // 이미지가 넘어왔으면 S3에 다시 올리고 dto 이미지 리스트를 새로 만든다
        if (images != null && !images.isEmpty()) {
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

            // 기존 이미지들은 일단 다 날린다 (물리 S3 삭제는 너네 정책대로)
            List<ProductImageEntity> oldImages = imageRepo.findAllByProduct_PdIdx(pdIdx);
            oldImages.forEach(imageRepo::delete);
        }

        // 공통 업데이트 로직 호출
        updateProductInternal(product, dto);
    }

    // =========================================================
    // 등록 (기존)
    // =========================================================
    @Transactional
    public Long register(Long userIdx, ProductCreateDTO dto) {

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

        ProductEntity product = productRepo.save(
                ProductEntity.builder()
                        .seller(seller)
                        .ctLow(low)
                        .pdTitle(dto.getTitle())
                        .pdPrice(dto.getPrice())
                        .pdContent(dto.getContent())
                        .pdLocation(dto.getLocation())
                        .pdStatus(dto.getPdStatus())
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
        ProductEntity savedProduct = productRepo.save(product);
        alarmService.createAlarmsForMatchingProduct(savedProduct);

        // 거래 기본값 저장
        DealEntity deal = DealEntity.builder()
                .product(product)
                .seller(seller)
                .buyer(null)
                .dDeal(dto.getDDeal())
                .dStatus(0L) // 판매중
                .dBuy(0L)
                .dSell(0L)
                .build();
        dealRepo.save(deal);

        return product.getPdIdx();
    }

    // =========================================================
    // 목록 조회 (삭제 제외)
    // =========================================================
    private Sort resolveSort(String sort) {
        String key = (sort == null || sort.isBlank()) ? "recent" : sort;
        return switch (key) {
            case "price_asc"  -> Sort.by(Sort.Direction.ASC, "pdPrice");
            case "price_desc" -> Sort.by(Sort.Direction.DESC, "pdPrice");
            default           -> Sort.by(Sort.Direction.DESC, "pdCreate");
        };
    }

    @Transactional(readOnly = true)
    public Page<ProductEntity> getProductsByIds(
            Long upperId, Long middleId, Long lowId,
            String sort, int page, int size
    ) {
        Pageable pageable = PageRequest.of(page, size, resolveSort(sort));
        return productRepo.findAllByCategoryIds(upperId, middleId, lowId, pageable);
    }

    @Transactional(readOnly = true)
    public Page<ProductEntity> getProductsByNames(
            String big, String mid, String sub,
            String sort, int page, int size
    ) {
        Pageable pageable = PageRequest.of(page, size, resolveSort(sort));
        return productRepo.findAllByNames(big, mid, sub, pageable);
    }

    // =========================================================
    // 내 상품 목록
    // =========================================================
    public List<productMyPageDTO> getMyProductByUIdx(Long uIdx, Integer status) {
        UserEntity user = userRepository.findById(uIdx)
                .orElseThrow(() ->
                        new ResponseStatusException(HttpStatus.NOT_FOUND,
                                "해당 회원이 없습니다. u_idx=" + uIdx)
                );

        log.info("/mypage uIdx={} -> user.u_id={}", uIdx, user.getUid());

        List<ProductEntity> products;

        if (status != null && (status == 0 || status == 1)) {
            products = productRepository.findBySellerAndPdStatus(user, status);
            log.info("status={} 인 상품 {}개", status, products.size());
        } else {
            products = productRepository.findBySeller(user);
            log.info("전체 상품 {}개", products.size());
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
                    dto.setPd_thumb(null);
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

        DealEntity deal = dealRepo.findByProduct_PdIdx(pdIdx).orElse(null);
        String dDeal = (deal != null) ? deal.getDDeal() : null;

        Double sellerManner = null;
        if (seller != null) {
            sellerManner = seller.getUManner();
        }

        return ProductDetailDTO.builder()
                .pdIdx(product.getPdIdx())
                .pdTitle(product.getPdTitle())
                .pdPrice(product.getPdPrice())
                .pdContent(product.getPdContent())
                .pdLocation(product.getPdLocation())
                .location(product.getPdLocation())
                .pdStatus(product.getPdStatus())
                .dDeal(dDeal)
                .pdThumb(product.getPdThumb())
                .images(imageUrls)
                .sellerId(seller != null ? seller.getUIdx() : null)
                // 프론트에서 sellerName 쓴다 했으니까 닉네임 넣어줌
                .sellerName(seller != null ? seller.getUnickname() : null)
                .sellerAvatar(seller != null ? seller.getUProfile() : null)
                .sellerManner(sellerManner)
                .upperName(upperName)
                .middleName(middleName)
                .lowName(lowName)
                .pdCreate(product.getPdCreate() != null ? product.getPdCreate().toString() : null)
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
    // 오너 전용 액션들
    // =========================================================
    private ProductEntity getOwnedProduct(Long pdIdx, Long userIdx) {
        ProductEntity product = productRepo.findById(pdIdx)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "상품을 찾을 수 없습니다."));
        if (product.getSeller() == null || !product.getSeller().getUIdx().equals(userIdx)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "본인 상품만 처리할 수 있습니다.");
        }
        return product;
    }

    @Transactional
    public void softDeleteProduct(Long pdIdx, Long userIdx) {
        ProductEntity product = getOwnedProduct(pdIdx, userIdx);
        product.setPdDel(true);
        productRepo.save(product);
    }

    @Transactional
    public void bumpProduct(Long pdIdx, Long userIdx) {
        ProductEntity product = getOwnedProduct(pdIdx, userIdx);
        product.setPdRefdate(LocalDateTime.now());
        productRepo.save(product);
    }

    @Transactional
    public void completeProduct(Long pdIdx, Long userIdx) {
        ProductEntity product = getOwnedProduct(pdIdx, userIdx);
        product.setPdStatus(1); // 판매완료
        productRepo.save(product);

        dealRepo.findByProduct_PdIdx(pdIdx).ifPresent(deal -> {
            deal.setDSell(1L);
            deal.setDStatus(1L);
            dealRepo.save(deal);
        });
    }

    // JSON으로만 수정할 때 여기로 옴
    @Transactional
    public void updateProduct(Long pdIdx, Long userIdx, ProductCreateDTO dto) {
        ProductEntity product = getOwnedProduct(pdIdx, userIdx);
        updateProductInternal(product, dto);
    }

    /**
     * 등록/수정 공통 내부 로직
     */
    private void updateProductInternal(ProductEntity product, ProductCreateDTO dto) {
        // 카테고리도 바꿀 수 있게 등록 때랑 똑같이 검증
        CtLowEntity low = ctLowRepo.findById(dto.getLowId())
                .orElseThrow(() -> new IllegalArgumentException("하위 카테고리를 찾을 수 없습니다."));
        CtMiddleEntity middle = low.getMiddle();
        if (middle == null || !middle.getMiddleIdx().equals(dto.getMiddleId())) {
            throw new IllegalArgumentException("중위 카테고리가 하위와 일치하지 않습니다.");
        }
        if (middle.getUpper() == null || !middle.getUpper().getUpperIdx().equals(dto.getUpperId())) {
            throw new IllegalArgumentException("상위 카테고리가 중위와 일치하지 않습니다.");
        }

        product.setCtLow(low);
        product.setPdTitle(dto.getTitle());
        product.setPdContent(dto.getContent());
        product.setPdPrice(dto.getPrice());
        product.setPdLocation(dto.getLocation());
        product.setPdStatus(dto.getPdStatus());
        product.setPdUpdate(LocalDateTime.now());

        // 이미지가 dto 안에 있으면 그걸로 교체 (멀티파트 수정에서 들어온다)
        if (dto.getImageUrls() != null && !dto.getImageUrls().isEmpty()) {
            product.setPdThumb(dto.getImageUrls().get(0));

            // 기존 이미지 전부 삭제 후 다시 저장하는 경우는 위의 updateMultipart 에서 한다
            dto.getImageUrls().stream()
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

        productRepo.save(product);
    }
}
