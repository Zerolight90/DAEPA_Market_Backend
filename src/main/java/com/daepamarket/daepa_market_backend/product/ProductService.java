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
import org.apache.catalina.User;
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
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepo;
    private final ProductImageRepository imageRepo;
    private final DealRepository dealRepo;

    private final UserRepository userRepo;
    private final CtLowRepository ctLowRepo;

    // ✅ 로컬 FileStorageService 대신 S3 사용
    private final S3Service s3Service;

    private final AlarmService alarmService;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final JwtProvider jwtProvider;

    /**
     * 멀티파트로 온 파일을 S3에 올리고, 생성된 S3 URL들을 DTO에 꽂아서
     * 기존 register(...) 로직을 그대로 재사용하는 메서드
     */
    @Transactional
    public Long registerMultipart(Long userIdx, ProductCreateDTO dto, List<MultipartFile> images) {
        // 1) 이미지가 아예 없으면 빈 리스트
        if (images == null || images.isEmpty()) {
            dto.setImageUrls(List.of());
            return register(userIdx, dto);
        }

        // 2) 이미지가 있으면 S3에 올리고 URL 얻기
        //    products/ 라는 폴더 아래에 넣는다고 가정
        List<String> urls = images.stream()
                .map(file -> {
                    try {
                        // 파일명 중복 방지용으로 UUID를 프리픽스로 하나 붙여주면 좋아
                        String folder = "products";
                        // S3Service 안에서 파일명 처리하면 여기서 안 해도 됨
                        return s3Service.uploadFile(file, folder);
                    } catch (IOException e) {
                        // 업로드 중 하나라도 실패하면 롤백시키고 싶으니까 런타임으로 감싸버림
                        throw new RuntimeException("S3 업로드 중 오류 발생: " + file.getOriginalFilename(), e);
                    }
                })
                .toList();

        // 3) DTO에 S3 URL을 심어서 기존 register 로직 재사용
        dto.setImageUrls(urls);

        return register(userIdx, dto);
    }

    /**
     * 원래 있던 상품 등록 로직
     * (S3이든 로컬이든 여기까지 오면 이미 dto.imageUrls 안에 최종 경로가 들어와 있다고 가정)
     */
    @Transactional
    public Long register(Long userIdx, ProductCreateDTO dto) {
        // 0) 판매자 조회
        UserEntity seller = userRepo.findById(userIdx)
                .orElseThrow(() -> new IllegalArgumentException("판매자를 찾을 수 없습니다."));

        // 1) 카테고리 계층 검증
        CtLowEntity low = ctLowRepo.findById(dto.getLowId())
                .orElseThrow(() -> new IllegalArgumentException("하위 카테고리를 찾을 수 없습니다."));
        CtMiddleEntity middle = low.getMiddle();
        if (middle == null || !middle.getMiddleIdx().equals(dto.getMiddleId())) {
            throw new IllegalArgumentException("중위 카테고리가 하위와 일치하지 않습니다.");
        }
        if (middle.getUpper() == null || !middle.getUpper().getUpperIdx().equals(dto.getUpperId())) {
            throw new IllegalArgumentException("상위 카테고리가 중위와 일치하지 않습니다.");
        }

        // 2) 상품 저장
        ProductEntity product = productRepo.save(
                ProductEntity.builder()
                        .seller(seller)
                        .ctLow(low)
                        .pdTitle(dto.getTitle())
                        .pdPrice(dto.getPrice())
                        .pdContent(dto.getContent())
                        .pdLocation(dto.getLocation())
                        .pdStatus(dto.getPdStatus())
                        // ✅ 썸네일은 첫 번째 이미지 S3 URL
                        .pdThumb(dto.getImageUrls() != null && !dto.getImageUrls().isEmpty()
                                ? dto.getImageUrls().get(0)
                                : null)
                        .pdHit(0)
                        .pdRef(0)
                        .pdCreate(LocalDateTime.now())
                        .pdUpdate(LocalDateTime.now())
                        .build()
        );

        // 3) 이미지 테이블 저장
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

        // 4) 알람 매칭
        ProductEntity savedProduct = productRepo.save(product);
        alarmService.createAlarmsForMatchingProduct(savedProduct);

        // 5) 거래 저장 — buyer 는 등록 시점에 비움(null)
        DealEntity deal = DealEntity.builder()
                .product(product)
                .seller(seller)
                .buyer(null)
                .dDeal(dto.getDDeal())     // "DELIVERY"/"MEET"
                .dStatus(0L)               // 0=판매중
                .build();
        dealRepo.save(deal);

        return product.getPdIdx();
    }

    // ==========================
    // 목록 조회
    // ==========================
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

    public List<productMyPageDTO> getMyProductByUIdx(Long uIdx, Integer status) {
        UserEntity user = userRepository.findById(uIdx)
                .orElseThrow(() ->
                        new ResponseStatusException(HttpStatus.NOT_FOUND,
                                "해당 회원이 없습니다. u_idx=" + uIdx)
                );

        log.info("getMyProductByUIdx uIdx={} -> user.u_id={}", uIdx, user.getUid());

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

    // ==========================
    // ✅ 단건 상세 조회
    // ==========================
    @Transactional(readOnly = true)
    public ProductDetailDTO getProductDetail(Long pdIdx) {

        // 1) 상품
        ProductEntity product = productRepo.findById(pdIdx)
                .orElseThrow(() -> new IllegalArgumentException("상품을 찾을 수 없습니다."));

        // 2) 판매자
        UserEntity seller = product.getSeller();

        // 3) 카테고리 (low → middle → upper)
        CtLowEntity low = product.getCtLow();
        CtMiddleEntity middle = low != null ? low.getMiddle() : null;
        String upperName = middle != null && middle.getUpper() != null
                ? middle.getUpper().getUpperCt()
                : null;
        String middleName = middle != null ? middle.getMiddleCt() : null;
        String lowName = low != null ? low.getLowCt() : null;

        // 4) 이미지 리스트 (S3 URL 이미 DB에 있음)
        List<String> imageUrls = imageRepo.findAllByProduct_PdIdx(pdIdx)
                .stream()
                .map(ProductImageEntity::getImageUrl)
                .toList();

        // 5) 거래 정보 (Deal)
        DealEntity deal = dealRepo.findByProduct_PdIdx(pdIdx).orElse(null);
        String dDeal = (deal != null) ? deal.getDDeal() : null;
        // 🟢 판매자 매너 (u_manner) 꺼내기
        // UserEntity에 메서드가 getUManner() 라는 이름일 가능성이 높아서 이렇게 씀
        Double sellerManner = null;
        if (seller != null) {
            // 네 엔티티가 getUManner() / getUManner 둘 중 하나일 텐데
            // 아래처럼 한 줄만 남겨서 쓰면 됨
            sellerManner = seller.getUManner();   // <- 이름 다르면 여기만 맞춰
        }
        // 6) DTO 만들기
        return ProductDetailDTO.builder()
                .pdIdx(product.getPdIdx())
                .pdTitle(product.getPdTitle())
                .pdPrice(product.getPdPrice())
                .pdContent(product.getPdContent())
                .pdLocation(product.getPdLocation())
                .location(product.getPdLocation())      // 프론트에서 location 으로도 읽게
                .pdStatus(product.getPdStatus())
                .dDeal(dDeal)
                .pdThumb(product.getPdThumb())
                .images(imageUrls)
                .sellerId(seller != null ? seller.getUIdx() : null)
                .sellerName(seller != null ? seller.getUname() : null)
                .sellerAvatar(seller != null ? seller.getUProfile() : null)
                .sellerManner(sellerManner)
                .upperName(upperName)
                .middleName(middleName)
                .lowName(lowName)
                .pdCreate(product.getPdCreate() != null ? product.getPdCreate().toString() : null)
                .build();
    }
}
