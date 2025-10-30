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
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

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

    /* 이하 조회쪽은 네 기존 코드 그대로 */
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
}
