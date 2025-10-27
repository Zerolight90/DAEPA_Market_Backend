package com.daepamarket.daepa_market_backend.domain.favorite;

import com.daepamarket.daepa_market_backend.domain.product.ProductEntity;
import com.daepamarket.daepa_market_backend.domain.user.UserEntity;
import org.apache.ibatis.annotations.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface FavoriteProductRepository extends JpaRepository<FavoriteProductEntity, Long> {

    // ✅ 특정 유저 + 상품 기준으로 찜 레코드 찾기
    Optional<FavoriteProductEntity> findByUserAndProduct(UserEntity user, ProductEntity product);

    // ✅ 특정 상품의 찜 개수 (fStatus == true 인 것만)
    long countByProductAndStatusTrue(ProductEntity product);

    @Query("""
        select f.product
        from FavoriteProductEntity f
        where f.user.uIdx = :userId
          and (f.status = true or f.status is null)
    """)
    List<ProductEntity> findLikedProductsByUserId(@Param("userId") Long userId);
}
