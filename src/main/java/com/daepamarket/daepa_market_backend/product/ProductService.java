package com.daepamarket.daepa_market_backend.product;

import com.daepamarket.daepa_market_backend.S3Service;
import com.daepamarket.daepa_market_backend.alarm.AlarmService;
import com.daepamarket.daepa_market_backend.chat.service.ChatService;
import com.daepamarket.daepa_market_backend.domain.Category.CtLowEntity;
import com.daepamarket.daepa_market_backend.domain.Category.CtLowRepository;
import com.daepamarket.daepa_market_backend.domain.Category.CtMiddleEntity;
import com.daepamarket.daepa_market_backend.domain.chat.ChatRoomEntity;
import com.daepamarket.daepa_market_backend.domain.chat.repository.ChatRoomRepository;
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
import java.util.Optional;

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

    private final ChatService chatService;
    private final ChatRoomRepository chatRoomRepository;

    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final JwtProvider jwtProvider;

    @Transactional
    public Long registerMultipart(Long userIdx, ProductCreateDTO dto, List<MultipartFile> images) {
        if (images == null || images.isEmpty()) {
            dto.setImageUrls(List.of());
            return register(userIdx, dto);
        }

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

        ProductEntity savedProduct = productRepo.save(product);
        alarmService.createAlarmsForMatchingProduct(savedProduct);

        DealEntity deal = DealEntity.builder()
                .product(product)
                .seller(seller)
                .buyer(null)
                .dDeal(dto.getDDeal())
                .dStatus(0L)
                .dSell(0L)
                .build();
        dealRepo.save(deal);

        return product.getPdIdx();
    }

    @Transactional
    public void updateMultipart(Long pdIdx, Long userIdx, ProductCreateDTO dto, List<MultipartFile> images) {

        ProductEntity product = getOwnedProduct(pdIdx, userIdx);

        List<String> finalImageUrls = new ArrayList<>();
        if (dto.getImageUrls() != null) {
            finalImageUrls.addAll(dto.getImageUrls());
        }

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

        updateProductInternal(product, dto, finalImageUrls);
    }

    @Transactional
    public void updateProduct(Long pdIdx, Long userIdx, ProductCreateDTO dto) {
        ProductEntity product = getOwnedProduct(pdIdx, userIdx);

        List<String> currentImageUrls = imageRepo.findAllByProduct_PdIdx(pdIdx)
                .stream()
                .map(ProductImageEntity::getImageUrl)
                .toList();

        updateProductInternal(product, dto, new ArrayList<>(currentImageUrls));
    }

    private void updateProductInternal(ProductEntity product, ProductCreateDTO dto, List<String> finalImageUrls) {

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

        if (!finalImageUrls.isEmpty()) {
            product.setPdThumb(finalImageUrls.get(0));
        } else {
            product.setPdThumb(null);
        }

        List<ProductImageEntity> currentImages = imageRepo.findAllByProduct_PdIdx(product.getPdIdx());

        for (ProductImageEntity img : currentImages) {
            if (!finalImageUrls.contains(img.getImageUrl())) {
                imageRepo.delete(img);
            }
        }

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
    }

    @Transactional(readOnly = true)
    public Page<ProductEntity> getProductsByIds(
            Long upperId,
            Long middleId,
            Long lowId,
            Long min,
            Long max,
            String dDeal,
            boolean excludeSold,
            String sort,
            int page,
            int size
    ) {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(3);

        if ("favorite".equalsIgnoreCase(sort)) {
            return productRepo.findAllByCategoryIdsOrderByFavoriteDesc(
                    upperId,
                    middleId,
                    lowId,
                    cutoff,
                    PageRequest.of(page, size)
            );
        }

        Pageable pageable = PageRequest.of(page, size, resolveSort(sort));
        return productRepo.findAllByCategoryIds(
                upperId,
                middleId,
                lowId,
                min,
                max,
                dDeal,
                excludeSold,
                cutoff,
                pageable
        );
    }

    @Transactional(readOnly = true)
    public Page<ProductEntity> getProductsByNames(
            String big,
            String mid,
            String sub,
            Long min,
            Long max,
            String dDeal,
            boolean excludeSold,
            String sort,
            int page,
            int size
    ) {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(3);

        if ("favorite".equalsIgnoreCase(sort)) {
            return productRepo.findAllByNamesOrderByFavoriteDesc(
                    big,
                    mid,
                    sub,
                    cutoff,
                    PageRequest.of(page, size)
            );
        }

        Pageable pageable = PageRequest.of(page, size, resolveSort(sort));
        return productRepo.findAllByNames(
                big,
                mid,
                sub,
                min,
                max,
                dDeal,
                excludeSold,
                cutoff,
                pageable
        );
    }

    public List<productMyPageDTO> getMyProductByUIdx(Long uIdx, Integer status) {
        UserEntity user = userRepository.findById(uIdx)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "해당 회원이 없습니다. u_idx=" + uIdx));

        List<ProductEntity> products;
        if (status != null && (status == 0 || status == 1)) {
            products = productRepository.findBySellerAndPdStatus(user, status);
        } else {
            products = productRepository.findBySeller(user);
        }

        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

        return products.stream()
                .map(p -> {
                    productMyPageDTO dto = new productMyPageDTO();
                    dto.setPd_idx(p.getPdIdx());
                    dto.setU_idx(user.getUIdx());
                    dto.setPd_status(String.valueOf(p.getPdStatus()));
                    dto.setPd_title(p.getPdTitle());
                    dto.setPd_price(p.getPdPrice() != null ? p.getPdPrice().intValue() : 0);
                    dto.setPd_create(p.getPdCreate() != null ? p.getPdCreate().format(fmt) : null);

                    dto.setPd_thumb(resolveThumbUrl(p.getPdThumb()));

                    dealRepo.findByProduct_PdIdx(p.getPdIdx())
                            .ifPresent(deal -> dto.setD_status(deal.getDStatus()));

                    return dto;

                })
                .toList();
    }

    // DB에 들어있는 값이 uploads/... 나 no-image.png 여도
// 프론트에는 항상 S3의 풀 URL만 나가게 정규화
    private String resolveThumbUrl(String raw) {
        // 1) 아예 없으면 기본 이미지
        if (raw == null || raw.isBlank()) {
            return "https://daepa-s3.s3.ap-northeast-2.amazonaws.com/products/KakaoTalk_20251104_145039505.jpg";
        }

        // 2) 이미 풀 URL이면 그대로
        if (raw.startsWith("http://") || raw.startsWith("https://")) {
            return raw;
        }

        // 3) 예전 로컬경로로 저장돼 있던 것들 → 프론트에서 절대 못 여니까 S3 기본이미지로 교체
        if (raw.startsWith("uploads/") || raw.equals("no-image.png")) {
            return "https://daepa-s3.s3.ap-northeast-2.amazonaws.com/products/KakaoTalk_20251104_145039505.jpg";
        }

        // 4) 그 외에는 S3 규칙에 맞춰서 붙여주기
        return "https://daepa-s3.s3.ap-northeast-2.amazonaws.com/" + raw;
    }



    // 이하 상세/연관/삭제/완료 등은 네가 보낸 그대로 ↓↓↓

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
        Long dSell = deal != null ? deal.getDSell() : 0L;
        Long dStatus = deal != null ? deal.getDStatus() : 0L;
        String dDeal = deal != null ? deal.getDDeal() : null;

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
                .ddeal(dDeal)
                .dsell(dSell)
                .dstatus(dStatus)
                .build();
    }

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
        getOwnedProduct(pdIdx, userIdx);
        ProductEntity product = getOwnedProduct(pdIdx, userIdx);
        dealRepo.findByProduct_PdIdx(pdIdx).ifPresent(deal -> {
            deal.setDSell(1L);
            dealRepo.save(deal);
            try {
                Long roomId = resolveRoomIdByDealOrProduct(deal.getDIdx(), pdIdx);
                if (roomId != null) {
                    String message = "📦 판매 완료 알림\n\n판매자가 상품을 [판매 완료] 상태로 변경했습니다.\n물품을 안전하게 전달받으셨다면, [구매 확정]을 눌러 거래를 완료해주세요! 👍";
                    chatService.sendMessage(roomId, userIdx, message, null, null);
                }
            } catch (Exception e) {
                log.error("판매 완료 채팅 알림 전송 중 오류 발생", e);
            }
        });
        product.setPdEdate(LocalDateTime.now());
        productRepo.save(product);
    }

    private ProductEntity getOwnedProduct(Long pdIdx, Long userIdx) {
        ProductEntity product = productRepo.findById(pdIdx)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "상품을 찾을 수 없습니다."));
        if (product.getSeller() == null || !product.getSeller().getUIdx().equals(userIdx)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "본인 상품만 처리할 수 있습니다.");
        }
        return product;
    }

    private Sort resolveSort(String sort) {
        String key = (sort == null || sort.isBlank()) ? "recent" : sort;
        return switch (key) {
            case "price_asc" -> Sort.by(Sort.Direction.ASC, "pdPrice");
            case "price_desc" -> Sort.by(Sort.Direction.DESC, "pdPrice");
            default -> Sort.by(Sort.Direction.DESC, "pdRefdate")
                    .and(Sort.by(Sort.Direction.DESC, "pdCreate"));
        };
    }

    private Long resolveRoomIdByDealOrProduct(Long dealId, Long productId) {
        if (dealId != null) {
            Optional<ChatRoomEntity> byDeal = chatRoomRepository.findByDealId(dealId);
            if (byDeal.isPresent()) return byDeal.get().getChIdx();
        }
        if (productId != null) {
            Optional<ChatRoomEntity> byProduct = chatRoomRepository.findLatestByProductId(productId);
            if (byProduct.isPresent()) return byProduct.get().getChIdx();
        }
        return null;
    }
}
