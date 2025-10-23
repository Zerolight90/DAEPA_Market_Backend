package com.daepamarket.daepa_market_backend.product;

import com.daepamarket.daepa_market_backend.alarm.AlarmService;
import com.daepamarket.daepa_market_backend.common.FileStorageService;
import com.daepamarket.daepa_market_backend.domain.Category.*;
import com.daepamarket.daepa_market_backend.domain.deal.DealEntity;
import com.daepamarket.daepa_market_backend.domain.deal.DealRepository;
import com.daepamarket.daepa_market_backend.domain.product.ProductEntity;
import com.daepamarket.daepa_market_backend.domain.product.ProductRepository;
import com.daepamarket.daepa_market_backend.domain.productimage.ProductImageEntity;
import com.daepamarket.daepa_market_backend.domain.productimage.ProductImageRepository;
import com.daepamarket.daepa_market_backend.domain.user.UserEntity;
import com.daepamarket.daepa_market_backend.domain.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepo;
    private final ProductImageRepository imageRepo;
    private final DealRepository dealRepo;

    private final UserRepository userRepo;
    private final CtLowRepository ctLowRepo;
    private final CtMiddleRepository middleRepo;

    private final FileStorageService fileStorageService; // ⬅️ 추가 주입

    private final ProductRepository productRepository;
    private final AlarmService alarmService;

    /** ⬇️ 새 멀티파트 엔드포인트에서 호출: 파일 저장 → URL 생성 → 기존 register 재사용 */
    @Transactional
    public Long registerMultipart(Long userIdx, ProductCreateDTO dto, List<MultipartFile> images) {
        System.out.println("[service] images in = " + (images == null ? -1 : images.size()));
        List<String> urls = (images == null || images.isEmpty())
                ? List.of()
                : fileStorageService.saveAll(images);
        System.out.println("[service] urls saved = " + urls.size());
        dto.setImageUrls(urls);
        return register(userIdx, dto);
    }


    /** (네가 제공한 기존 메서드) 상품 등록 (Product + ProductImage + Deal[buyer=null]) */
    @Transactional
    public Long register(Long userIdx, ProductCreateDTO dto) {
        // 0) 판매자 로딩
        UserEntity seller = userRepo.findById(userIdx)
                .orElseThrow(() -> new IllegalArgumentException("판매자를 찾을 수 없습니다."));

        // 1) 카테고리 계층 검증 (하위 → 중위 → 상위)
        CtLowEntity low = ctLowRepo.findById(dto.getLowId())
                .orElseThrow(() -> new IllegalArgumentException("하위 카테고리를 찾을 수 없습니다."));
        CtMiddleEntity middle = low.getMiddle();
        if (middle == null || !middle.getMiddleIdx().equals(dto.getMiddleId())) {
            throw new IllegalArgumentException("선택한 중위 카테고리가 하위와 일치하지 않습니다.");
        }
        if (middle.getUpper() == null || !middle.getUpper().getUpperIdx().equals(dto.getUpperId())) {
            throw new IllegalArgumentException("선택한 상위 카테고리가 중위와 일치하지 않습니다.");
        }

        // 2) 상품 저장 — 연관관계를 엔티티로 설정
        ProductEntity product = productRepo.save(
                ProductEntity.builder()
                        .seller(seller)
                        .ctLow(low)
                        .pdTitle(dto.getTitle())
                        .pdPrice(dto.getPrice())
                        .pdContent(dto.getContent())
                        .pdLocation(dto.getLocation())
                        .pdStatus(dto.getPdStatus()) // 0/1
                        .pdThumb(dto.getImageUrls()!=null && !dto.getImageUrls().isEmpty()
                                ? dto.getImageUrls().get(0) : null)
                        .pdHit(0)
                        .pdRef(0)
                        .pdCreate(LocalDateTime.now())
                        .pdUpdate(LocalDateTime.now())
                        .build()
        );

        // 3) 이미지 저장
        // 3) 이미지 저장
        List<String> urls = dto.getImageUrls();
        if (urls != null && !urls.isEmpty()) {
            urls.stream().limit(10).forEach(url -> imageRepo.save(
                    ProductImageEntity.builder()
                            .product(product)
                            .imageUrl(url)
                            .createdAt(LocalDateTime.now())
                            .updatedAt(LocalDateTime.now())
                            .build()
            ));
            System.out.println("[register] images inserted = " + Math.min(urls.size(), 10));
        } else {
            System.out.println("[register] no images to insert (imageUrls is null/empty)");
        }

        ProductEntity savedProduct = productRepository.save(product);
        alarmService.createAlarmsForMatchingProduct(savedProduct);

        // 4) 거래 저장 — buyer 는 등록 시점에 비움(null)
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
    public Page<ProductEntity> getProductsByIds(Long upperId, Long middleId, Long lowId,
                                                String sort, int page, int size) {
        Pageable pageable = switch (sort) {
            case "price_asc"  -> PageRequest.of(page, size, Sort.by("price").ascending());
            case "price_desc" -> PageRequest.of(page, size, Sort.by("price").descending());
            default           -> PageRequest.of(page, size, Sort.by("createdAt").descending());
        };
        return productRepo.findAllByCategoryIds(upperId, middleId, lowId, pageable);
    }

    @Transactional(readOnly = true)
    public Page<ProductEntity> getProductsByNames(
            String big,
            String mid,
            String sub,
            String sort,
            int page,
            int size
    ) {
        Sort sortSpec = switch (sort) {
            case "priceAsc"  -> Sort.by(Sort.Direction.ASC,  "pdPrice");
            case "priceDesc" -> Sort.by(Sort.Direction.DESC, "pdPrice");
            case "old"       -> Sort.by(Sort.Direction.ASC,  "pdCreate");
            default          -> Sort.by(Sort.Direction.DESC, "pdCreate"); // recent
        };

        Pageable pageable = PageRequest.of(page, size, sortSpec);
        return productRepo.findAllByNames(big, mid, sub, pageable);
    }

}