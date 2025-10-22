package com.daepamarket.daepa_market_backend.domain.product;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProductRepository extends JpaRepository<ProductEntity, Long> {

    /** ✅ ID로 필터: upperIdx/middleIdx/lowIdx 전부 선택적 */
    @Query("""
        SELECT p FROM ProductEntity p
          JOIN p.ctLow l
          JOIN l.middle m
          JOIN m.upper  u
        WHERE (:upperId  IS NULL OR u.upperIdx   = :upperId)
          AND (:middleId IS NULL OR m.middleIdx  = :middleId)
          AND (:lowId    IS NULL OR l.lowIdx     = :lowId)
    """)
    Page<ProductEntity> findAllByCategoryIds(
            @Param("upperId")  Long upperId,
            @Param("middleId") Long middleId,
            @Param("lowId")    Long lowId,
            Pageable pageable
    );

    /** (옵션) 이름으로 필터: upperCt/middleCt/lowCt 전부 선택적 */
    @Query("""
           select p from ProductEntity p
           where (:big is null or p.ctLow.middle.upper.upperCt = :big)
             and (:mid is null or p.ctLow.middle.middleCt     = :mid)
             and (:sub is null or p.ctLow.lowCt               = :sub)
           """)
    Page<ProductEntity> findAllByNames(
            @Param("big") String big,
            @Param("mid") String mid,
            @Param("sub") String sub,
            Pageable pageable
    );
}
