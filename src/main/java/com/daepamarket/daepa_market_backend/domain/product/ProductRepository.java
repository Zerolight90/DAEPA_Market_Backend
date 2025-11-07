package com.daepamarket.daepa_market_backend.domain.product;

import com.daepamarket.daepa_market_backend.domain.user.UserEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
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
          AND (p.pdEdate IS NULL OR p.pdEdate >= :cutoff)
        """)
    Page<ProductEntity> findAllByCategoryIds(
            @Param("upperId")  Long upperId,
            @Param("middleId") Long middleId,
            @Param("lowId")    Long lowId,
            @Param("cutoff") LocalDateTime cutoff,
            Pageable pageable
    );

    @Query("""
           SELECT p FROM ProductEntity p
           WHERE (:big IS NULL OR p.ctLow.middle.upper.upperCt = :big)
             AND (:mid IS NULL OR p.ctLow.middle.middleCt     = :mid)
             AND (:sub IS NULL OR p.ctLow.lowCt               = :sub)
             AND p.pdDel = false
             AND (p.pdEdate IS NULL OR p.pdEdate >= :cutoff)
           """)
    Page<ProductEntity> findAllByNames(
            @Param("big") String big,
            @Param("mid") String mid,
            @Param("sub") String sub,
            @Param("cutoff") LocalDateTime cutoff,
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

    // ✅ 새로 넣는 "찜 많은 순" 버전
    // ─────────────────────────────────────────
    @Query("""
        select p
        from ProductEntity p
            join p.ctLow low
            join low.middle mid
            join mid.upper up
            left join FavoriteProductEntity f on f.product = p
        where (:upperId is null or up.upperIdx = :upperId)
          and (:middleId is null or mid.middleIdx = :middleId)
          and (:lowId is null or low.lowIdx = :lowId)
          and (p.pdEdate is null or p.pdEdate >= :cutoff)
          and p.pdDel = false
        group by p
        order by count(f) desc, p.pdRefdate desc, p.pdCreate desc
        """)
    Page<ProductEntity> findAllByCategoryIdsOrderByFavoriteDesc(
            @Param("upperId") Long upperId,
            @Param("middleId") Long middleId,
            @Param("lowId") Long lowId,
            @Param("cutoff") LocalDateTime cutoff,
            Pageable pageable
    );

    // ─────────────────────────────────────────
    // 이름으로 찾는 애도 하나 더 (홈에서 혹시 쓸 수도 있으니까)
    // ─────────────────────────────────────────
    @Query("""
        select p
        from ProductEntity p
            join p.ctLow low
            join low.middle mid
            join mid.upper up
            left join FavoriteProductEntity f on f.product = p 
        where (:big is null or up.upperCt = :big)
          and (:mid is null or mid.middleCt = :mid)
          and (:sub is null or low.lowCt = :sub)
          and (p.pdEdate is null or p.pdEdate >= :cutoff)
          and p.pdDel = false
        group by p
        order by count(f) desc, p.pdRefdate desc, p.pdCreate desc
        """)
    Page<ProductEntity> findAllByNamesOrderByFavoriteDesc(
            @Param("big") String big,
            @Param("mid") String mid,
            @Param("sub") String sub,
            @Param("cutoff") LocalDateTime cutoff,
            Pageable pageable
    );

    // 최근 등록 상품 조회 (관리자 대시보드용)
    @Query("""
        SELECT p FROM ProductEntity p
        WHERE p.pdDel = false
        ORDER BY p.pdCreate DESC
        """)
    List<ProductEntity> findRecentProducts(Pageable pageable);
}
