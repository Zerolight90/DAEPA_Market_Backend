package com.daepamarket.daepa_market_backend.favorite;

import com.daepamarket.daepa_market_backend.domain.favorite.FavoriteProductEntity;
import com.daepamarket.daepa_market_backend.domain.favorite.FavoriteProductRepository;
import com.daepamarket.daepa_market_backend.domain.product.ProductEntity;
import com.daepamarket.daepa_market_backend.domain.product.ProductRepository;
import com.daepamarket.daepa_market_backend.domain.productimage.ProductImageRepository;
import com.daepamarket.daepa_market_backend.domain.user.UserEntity;
import com.daepamarket.daepa_market_backend.domain.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class FavoriteService {

    private final FavoriteProductRepository favoriteRepo;
    private final UserRepository userRepo;
    private final ProductRepository productRepo;
    private final ProductImageRepository imageRepo;

    /**
     * ✅ 찜 토글 기능
     * - 이미 찜했다면 → 해제
     * - 처음 누르면 → 등록
     * - 현재 상태(true/false)를 반환
     */
    @Transactional
    public boolean toggle(Long userId, Long productId) {
        UserEntity user = userRepo.findById(userId).orElseThrow();
        ProductEntity product = productRepo.findById(productId).orElseThrow();

        // 기존 찜 여부 확인
        var favOpt = favoriteRepo.findByUserAndProduct(user, product);
        if (favOpt.isEmpty()) {
            // 찜 기록이 없으면 새로 추가 (찜 상태 true)
            favoriteRepo.save(
                    FavoriteProductEntity.builder()
                            .user(user).product(product)
                            .status(true)
                            .fDate(LocalDateTime.now())
                            .build()
            );
            return true;
        }

        // 이미 존재한다면 상태 토글 (true↔false)
        FavoriteProductEntity fav = favOpt.get();
        boolean next = !Boolean.TRUE.equals(fav.getStatus());
        fav.setStatus(next);
        fav.setFDate(LocalDateTime.now());
        return next;
    }

    /** ✅ 현재 유저가 이 상품을 찜했는지 여부 조회 */
    @Transactional(readOnly = true)
    public boolean isFavorited(Long userId, Long productId) {
        UserEntity user = userRepo.findById(userId).orElseThrow();
        ProductEntity product = productRepo.findById(productId).orElseThrow();
        return favoriteRepo.findByUserAndProduct(user, product)
                .map(FavoriteProductEntity::getStatus)
                .orElse(false);
    }

    /** ✅ 상품의 총 찜 수 조회 */
    @Transactional(readOnly = true)
    public long count(Long productId) {
        ProductEntity product = productRepo.findById(productId).orElseThrow();
        return favoriteRepo.countByProductAndStatusTrue(product);
    }

    public List<FavoriteItemDTO> getMyFavorites(Long userId) {
        List<ProductEntity> products = favoriteRepo.findLikedProductsByUserId(userId);

        return products.stream().map(p -> {
            // 대표 이미지 1장 가져오기
            String imageUrl = imageRepo.findTop1ByProductOrderByPiIdxAsc(p)
                    .map(img -> img.getImageUrl()) // 컬럼명이 imageUrl임
                    .orElse(null);

            return FavoriteItemDTO.builder()
                    .id(p.getPdIdx())
                    .title(p.getPdTitle())
                    .price(p.getPdPrice())
                    .imageUrl(imageUrl)
                    .build();
        }).toList();
    }
}
