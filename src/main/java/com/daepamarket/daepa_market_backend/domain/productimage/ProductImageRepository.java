package com.daepamarket.daepa_market_backend.domain.productimage;

import com.daepamarket.daepa_market_backend.domain.product.ProductEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProductImageRepository extends JpaRepository<ProductImageEntity, Long> {
    Optional<ProductImageEntity> findTop1ByProductOrderByPiIdxAsc(ProductEntity product);
    List<ProductImageEntity> findAllByProduct_PdIdx(Long pdIdx);
}
