package com.daepamarket.daepa_market_backend.domain.product;

import com.daepamarket.daepa_market_backend.domain.user.UserEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ProductRepository extends JpaRepository<ProductEntity, Long>, JpaSpecificationExecutor<ProductEntity> {

    @Query("""
        SELECT p FROM ProductEntity p
          JOIN p.ctLow l
          JOIN l.middle m
          JOIN m.upper  u
        WHERE (:upperId  IS NULL OR u.upperIdx   = :upperId)
          AND (:middleId IS NULL OR m.middleIdx  = :middleId)
          AND (:lowId    IS NULL OR l.lowIdx     = :lowId)
          AND p.pdDel = false
        """)
    Page<ProductEntity> findAllByCategoryIds(
            @Param("upperId")  Long upperId,
            @Param("middleId") Long middleId,
            @Param("lowId")    Long lowId,
            Pageable pageable
    );

    @Query("""
           SELECT p FROM ProductEntity p
           WHERE (:big IS NULL OR p.ctLow.middle.upper.upperCt = :big)
             AND (:mid IS NULL OR p.ctLow.middle.middleCt     = :mid)
             AND (:sub IS NULL OR p.ctLow.lowCt               = :sub)
             AND p.pdDel = false
           """)
    Page<ProductEntity> findAllByNames(
            @Param("big") String big,
            @Param("mid") String mid,
            @Param("sub") String sub,
            Pageable pageable
    );

    List<ProductEntity> findBySeller(UserEntity user);

    List<ProductEntity> findBySellerAndPdStatus(UserEntity user, int pdStatus);

    @Query("""
       SELECT p
       FROM ProductEntity p
       WHERE p.seller.uIdx = :sellerId
       ORDER BY p.pdIdx DESC
       """)
    Page<ProductEntity> findPageBySellerId(@Param("sellerId") Long sellerId, Pageable pageable);

    @Query("""
        SELECT p FROM ProductEntity p
        WHERE p.pdDel = false
          AND (:lowId IS NULL OR p.ctLow.lowIdx = :lowId)
          AND p.pdIdx <> :excludeId
        ORDER BY p.pdCreate DESC
        """)
    Page<ProductEntity> findRelatedByLowIdExcludingSelf(
            @Param("lowId") Long lowId,
            @Param("excludeId") Long excludeId,
            Pageable pageable
    );
}
