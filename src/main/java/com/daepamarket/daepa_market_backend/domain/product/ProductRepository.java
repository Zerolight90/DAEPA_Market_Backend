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

    // ✅ 카테고리 id + 가격 필터
    @Query("""
        SELECT p FROM ProductEntity p
          JOIN p.ctLow l
          JOIN l.middle m
          JOIN m.upper  u
        WHERE (:upperId  IS NULL OR u.upperIdx   = :upperId)
          AND (:middleId IS NULL OR m.middleIdx  = :middleId)
          AND (:lowId    IS NULL OR l.lowIdx     = :lowId)
          AND (:min IS NULL OR p.pdPrice >= :min)
          AND (:max IS NULL OR p.pdPrice <= :max)
          AND p.pdDel = false
          AND (p.pdEdate IS NULL OR p.pdEdate >= :cutoff)
        """)
    Page<ProductEntity> findAllByCategoryIds(
            @Param("upperId")  Long upperId,
            @Param("middleId") Long middleId,
            @Param("lowId")    Long lowId,
            @Param("min")      Long min,
            @Param("max")      Long max,
            @Param("cutoff")   LocalDateTime cutoff,
            Pageable pageable
    );

    // ✅ 카테고리 이름 + 가격 필터
    @Query("""
           SELECT p FROM ProductEntity p
           WHERE (:big IS NULL OR p.ctLow.middle.upper.upperCt = :big)
             AND (:mid IS NULL OR p.ctLow.middle.middleCt     = :mid)
             AND (:sub IS NULL OR p.ctLow.lowCt               = :sub)
             AND (:min IS NULL OR p.pdPrice >= :min)
             AND (:max IS NULL OR p.pdPrice <= :max)
             AND p.pdDel = false
             AND (p.pdEdate IS NULL OR p.pdEdate >= :cutoff)
           """)
    Page<ProductEntity> findAllByNames(
            @Param("big") String big,
            @Param("mid") String mid,
            @Param("sub") String sub,
            @Param("min") Long min,
            @Param("max") Long max,
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

    // 찜 많은 순 (id 기준)
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

    // 찜 많은 순 (이름 기준)
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

    @Query("""
        SELECT p FROM ProductEntity p
        WHERE p.pdDel = false
        ORDER BY p.pdCreate DESC
        """)
    List<ProductEntity> findRecentProducts(Pageable pageable);

    @Query("""
           SELECT p FROM ProductEntity p
           WHERE p.ctLow.middle.upper.upperCt = :upperCategory
             AND p.ctLow.middle.middleCt = :middleCategory
             AND p.ctLow.lowCt = :lowCategory
             AND p.pdPrice >= :minPrice
             AND p.pdPrice <= :maxPrice
             AND p.pdDel = false
             AND (p.pdEdate IS NULL OR p.pdEdate >= :now)
           """)
    List<ProductEntity> findMatchingProducts(
            @Param("upperCategory") String upperCategory,
            @Param("middleCategory") String middleCategory,
            @Param("lowCategory") String lowCategory,
            @Param("minPrice") int minPrice,
            @Param("maxPrice") int maxPrice,
            @Param("now") LocalDateTime now
    );
}
