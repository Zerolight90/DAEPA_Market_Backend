package com.daepamarket.daepa_market_backend.favorite;

import com.daepamarket.daepa_market_backend.domain.favorite.FavoriteProductEntity;
import com.daepamarket.daepa_market_backend.domain.favorite.FavoriteProductRepository;
import com.daepamarket.daepa_market_backend.domain.product.ProductEntity;
import com.daepamarket.daepa_market_backend.domain.product.ProductRepository;
import com.daepamarket.daepa_market_backend.domain.user.UserEntity;
import com.daepamarket.daepa_market_backend.domain.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import static org.springframework.data.domain.Sort.Direction.DESC;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
public class FavoriteService {
    private final FavoriteProductRepository favoriteRepo;
    private final UserRepository userRepo;
    private final ProductRepository productRepo;

    @Transactional
    public boolean toggle(Long userId, Long productId) {
        UserEntity user = userRepo.findById(userId).orElseThrow();
        ProductEntity product = productRepo.findById(productId).orElseThrow();

        var favOpt = favoriteRepo.findByUserAndProduct(user, product);
        if (favOpt.isEmpty()) {
            favoriteRepo.save(
                    FavoriteProductEntity.builder()
                            .user(user).product(product)
                            .status(true)
                            .fDate(LocalDateTime.now())
                            .build()
            );
            return true;
        }

        FavoriteProductEntity fav = favOpt.get();
        boolean next = !Boolean.TRUE.equals(fav.getStatus());
        fav.setStatus(next);
        fav.setFDate(LocalDateTime.now());
        return next;
    }

    @Transactional(readOnly = true)
    public boolean isFavorited(Long userId, Long productId) {
        UserEntity user = userRepo.findById(userId).orElseThrow();
        ProductEntity product = productRepo.findById(productId).orElseThrow();
        return favoriteRepo.findByUserAndProduct(user, product)
                .map(FavoriteProductEntity::getStatus)
                .orElse(false);
    }

    @Transactional(readOnly = true)
    public long count(Long productId) {
        ProductEntity product = productRepo.findById(productId).orElseThrow();
        return favoriteRepo.countByProductAndStatusIsTrue(product);
    }

    // =========================
    // ✅ 내가 찜한 상품 목록
    // =========================
    @Transactional(readOnly = true)
    public List<FavoriteItemDTO> list(Long userId) {
        UserEntity user = userRepo.findById(userId).orElseThrow();

        // ⚠ 여기 "fDate" 는 엔티티 필드명(게터명)과 100% 동일해야 함
        var favs = favoriteRepo.findByUserAndStatus(user, true, Sort.by(DESC, "fDate"));

        return favs.stream()
                .map(f -> toDto(f.getProduct()))
                .toList();
    }

    // ProductEntity -> FavoriteItemDTO 매핑
    private FavoriteItemDTO toDto(ProductEntity p) {
        // createdAt 문자열로 간단히 포맷 (프론트에서 그대로 표시 가능)
        String createdAt = null;
        if (p.getPdCreate() != null) {
            createdAt = p.getPdCreate().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        }

        return FavoriteItemDTO.builder()
                .id(p.getPdIdx())
                .title(p.getPdTitle())
                .price(p.getPdPrice())
                .imageUrl(p.getPdThumb())      // 썸네일 컬럼명에 맞춰 사용
                .build();
    }
}
