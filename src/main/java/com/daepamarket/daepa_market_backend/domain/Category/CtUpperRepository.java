package com.daepamarket.daepa_market_backend.domain.Category;

import java.util.List;
import java.util.Optional;

import com.daepamarket.daepa_market_backend.category.UpperCategoryWithCountDTO;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface CtUpperRepository extends JpaRepository<CtUpperEntity, Long> {

    Optional<CtUpperEntity> findByUpperCt(String upperCt);

    @Query("""
         SELECT new com.daepamarket.daepa_market_backend.category.UpperCategoryWithCountDTO(
           u.upperIdx,
           u.upperCt,
           COUNT(p.pdIdx)
       )
       FROM CtUpperEntity u
       LEFT JOIN u.middles m
       LEFT JOIN m.lowers l
       LEFT JOIN com.daepamarket.daepa_market_backend.domain.product.ProductEntity p
              ON p.ctLow = l
       GROUP BY u.upperIdx, u.upperCt
       ORDER BY u.upperIdx
    """)
    List<UpperCategoryWithCountDTO> findAllWithProductCount();
}
