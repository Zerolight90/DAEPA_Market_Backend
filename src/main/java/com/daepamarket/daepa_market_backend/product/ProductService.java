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
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepo;
    private final ProductImageRepository imageRepo;
    private final DealRepository dealRepo;

    private final UserRepository userRepo;
    private final CtLowRepository ctLowRepo;
    private final CtMiddleRepository middleRepo;

    private final FileStorageService fileStorageService;
    private final AlarmService alarmService;

    @Transactional
    public Long registerMultipart(Long userIdx, ProductCreateDTO dto, List<MultipartFile> images) {
        List<String> urls = (images == null || images.isEmpty())
                ? List.of()
                : fileStorageService.saveAll(images);
        dto.setImageUrls(urls);
        return register(userIdx, dto);
    }

    @Transactional
    public Long register(Long userIdx, ProductCreateDTO dto) {
        UserEntity seller = userRepo.findById(userIdx)
                .orElseThrow(() -> new IllegalArgumentException("판매자를 찾을 수 없습니다."));

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
                                ? dto.getImageUrls().get(0) : null)
                        .pdHit(0)
                        .pdRef(0)
                        .pdCreate(LocalDateTime.now())
                        .pdUpdate(LocalDateTime.now())
                        .build()
        );

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
        }

        DealEntity deal = DealEntity.builder()
                .product(product)
                .seller(seller)
                .buyer(null)
                .dDeal(dto.getDDeal())
                .dStatus(0L)
                .build();
        dealRepo.save(deal);

        return product.getPdIdx();
    }

    /* ✅ 정렬 키 매핑: recent|price_asc|price_desc → pdCreate/pdPrice */
    private Sort resolveSort(String sort) {
        String key = (sort == null || sort.isBlank()) ? "recent" : sort;
        return switch (key) {
            case "price_asc"  -> Sort.by(Sort.Direction.ASC,  "pdPrice");
            case "price_desc" -> Sort.by(Sort.Direction.DESC, "pdPrice");
            default           -> Sort.by(Sort.Direction.DESC, "pdCreate");
        };
    }

    /** ✅ ID 기반 조회 */
    @Transactional(readOnly = true)
    public Page<ProductEntity> getProductsByIds(
            Long upperId, Long middleId, Long lowId,
            String sort, int page, int size
    ) {
        Pageable pageable = PageRequest.of(page, size, resolveSort(sort));
        return productRepo.findAllByCategoryIds(upperId, middleId, lowId, pageable);
    }

    /** ✅ 이름 기반 조회 */
    @Transactional(readOnly = true)
    public Page<ProductEntity> getProductsByNames(
            String big, String mid, String sub,
            String sort, int page, int size
    ) {
        Pageable pageable = PageRequest.of(page, size, resolveSort(sort));
        return productRepo.findAllByNames(big, mid, sub, pageable);
    }
}
