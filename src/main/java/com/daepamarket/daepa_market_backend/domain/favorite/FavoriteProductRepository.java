package com.daepamarket.daepa_market_backend.domain.favorite;

import com.daepamarket.daepa_market_backend.domain.product.ProductEntity;
import com.daepamarket.daepa_market_backend.domain.user.UserEntity;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FavoriteProductRepository extends JpaRepository<FavoriteProductEntity, Long> {

    // 특정 유저 + 상품 기준으로 찜 레코드 찾기
    Optional<FavoriteProductEntity> findByUserAndProduct(UserEntity user, ProductEntity product);

    // 특정 상품의 찜 개수(찜 상태 true인 것만)
    long countByProductAndStatusIsTrue(ProductEntity product);
    // ✅ 유저가 찜한 목록(찜 상태 true) 최신순
    List<FavoriteProductEntity> findByUserAndStatus(UserEntity user, boolean status, Sort sort);
}
