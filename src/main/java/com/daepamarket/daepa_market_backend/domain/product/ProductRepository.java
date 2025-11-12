package com.daepamarket.daepa_market_backend.domain.product;

import com.daepamarket.daepa_market_backend.admin.product.AdminProductProjection;
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
        SELECT p
        FROM ProductEntity p
            JOIN p.ctLow l
            JOIN l.middle m
            JOIN m.upper u
            LEFT JOIN DealEntity d ON d.product = p
        WHERE (:upperId  IS NULL OR u.upperIdx   = :upperId)
          AND (:middleId IS NULL OR m.middleIdx  = :middleId)
          AND (:lowId    IS NULL OR l.lowIdx     = :lowId)
          AND (:min      IS NULL OR p.pdPrice >= :min)
          AND (:max      IS NULL OR p.pdPrice <= :max)
          AND (
                :dDeal IS NULL
                OR (
                    d.dDeal IS NOT NULL
                    AND LOWER(d.dDeal) LIKE LOWER(CONCAT(:dDeal, '%'))
                )
              )
          AND (
                :excludeSold = false
                OR d.dSell IS NULL
                OR d.dSell = 0
              )
          AND (
                :keyword IS NULL
                OR LOWER(p.pdTitle) LIKE LOWER(CONCAT('%', :keyword, '%'))
                OR LOWER(p.pdContent) LIKE LOWER(CONCAT('%', :keyword, '%'))
              )
          AND p.pdDel = false
          AND (p.pdEdate IS NULL OR p.pdEdate >= :cutoff)
        """)
    Page<ProductEntity> findAllByCategoryIds(
            @Param("upperId") Long upperId,
            @Param("middleId") Long middleId,
            @Param("lowId") Long lowId,
            @Param("min") Long min,
            @Param("max") Long max,
            @Param("dDeal") String dDeal,
            @Param("excludeSold") boolean excludeSold,
            @Param("keyword") String keyword,
            @Param("cutoff") LocalDateTime cutoff,
            Pageable pageable
    );

    @Query("""
        SELECT p
        FROM ProductEntity p
            JOIN p.ctLow l
            JOIN l.middle m
            JOIN m.upper u
            LEFT JOIN DealEntity d ON d.product = p
        WHERE (:big IS NULL OR u.upperCt   = :big)
          AND (:mid IS NULL OR m.middleCt  = :mid)
          AND (:sub IS NULL OR l.lowCt     = :sub)
          AND (:min IS NULL OR p.pdPrice >= :min)
          AND (:max IS NULL OR p.pdPrice <= :max)
          AND (
                :dDeal IS NULL
                OR (
                    d.dDeal IS NOT NULL
                    AND LOWER(d.dDeal) LIKE LOWER(CONCAT(:dDeal, '%'))
                )
              )
          AND (
                :excludeSold = false
                OR d.dSell IS NULL
                OR d.dSell = 0
              )
          AND p.pdDel = false
          AND (p.pdEdate IS NULL OR p.pdEdate >= :cutoff)
        """)
    Page<ProductEntity> findAllByNames(
            @Param("big") String big,
            @Param("mid") String mid,
            @Param("sub") String sub,
            @Param("min") Long min,
            @Param("max") Long max,
            @Param("dDeal") String dDeal,
            @Param("excludeSold") boolean excludeSold,
            @Param("cutoff") LocalDateTime cutoff,
            Pageable pageable
    );

    // ✨ 기존: 전체를 다 가져오던 걸 살려두고,
    // ✨ 새로 "살아있는" 것만 가져오는 버전 추가
    @Query("""
       SELECT p
       FROM ProductEntity p
       WHERE p.seller.uIdx = :sellerId
       ORDER BY p.pdIdx DESC
       """)
    Page<ProductEntity> findPageBySellerId(
            @Param("sellerId") Long sellerId,
            Pageable pageable
    );

    // ✨ 여기 추가: pdDel=false + 3일 이내
    @Query("""
       SELECT p
       FROM ProductEntity p
       WHERE p.seller.uIdx = :sellerId
         AND p.pdDel = false
         AND (p.pdEdate IS NULL OR p.pdEdate >= :cutoff)
       ORDER BY p.pdIdx DESC
       """)
    Page<ProductEntity> findAlivePageBySellerId(
            @Param("sellerId") Long sellerId,
            @Param("cutoff") LocalDateTime cutoff,
            Pageable pageable
    );

    List<ProductEntity> findBySeller(UserEntity user);

    List<ProductEntity> findBySellerAndPdStatus(UserEntity user, int pdStatus);

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
          and (
                :keyword IS NULL
                OR LOWER(p.pdTitle) LIKE LOWER(CONCAT('%', :keyword, '%'))
                OR LOWER(p.pdContent) LIKE LOWER(CONCAT('%', :keyword, '%'))
              )
          and (p.pdEdate is null or p.pdEdate >= :cutoff)
          and p.pdDel = false
        group by p
        order by count(f) desc, p.pdRefdate desc, p.pdCreate desc
        """)
    Page<ProductEntity> findAllByCategoryIdsOrderByFavoriteDesc(
            @Param("upperId") Long upperId,
            @Param("middleId") Long middleId,
            @Param("lowId") Long lowId,
            @Param("keyword") String keyword,
            @Param("cutoff") LocalDateTime cutoff,
            Pageable pageable
    );

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

    @Query(value = """
        SELECT 
            p.pd_idx as pdIdx,
            p.pd_title as pdTitle,
            p.pd_price as pdPrice,
            p.pd_thumb as pdThumb,
            p.pd_create as pdCreate,
            u.u_idx as sellerId,
            u.u_name as sellerName,
            up.upper_ct as upperCt,
            mid.middle_ct as middleCt,
            low.low_ct as lowCt,
            COUNT(DISTINCT n.ng_idx) as reportCount,
            CAST(COALESCE(d.d_status, 0) AS SIGNED) as dealStatus,
            CAST(COALESCE(d.d_sell, 0) AS SIGNED) as dealSell
        FROM product p
        LEFT JOIN user u ON p.u_idx = u.u_idx
        LEFT JOIN ct_low low ON p.ct_low = low.low_idx
        LEFT JOIN ct_middle mid ON low.middle_idx = mid.middle_idx
        LEFT JOIN ct_upper up ON mid.upper_idx = up.upper_idx
        LEFT JOIN deal d ON p.pd_idx = d.pd_idx
        LEFT JOIN naga n ON p.pd_idx = n.s_idx
        WHERE p.pd_del = false
          AND (:status IS NULL 
            OR (:status = 'ON_SALE' AND (d.d_sell IS NULL OR d.d_sell = 0) AND (d.d_status IS NULL OR d.d_status = 0))
            OR (:status = 'SOLD_OUT' AND ((d.d_sell IS NOT NULL AND d.d_sell > 0) OR (d.d_status IS NOT NULL AND d.d_status > 0))))
        GROUP BY p.pd_idx, p.pd_title, p.pd_price, p.pd_thumb, p.pd_create, 
                 u.u_idx, u.u_name, up.upper_ct, mid.middle_ct, low.low_ct, 
                 d.d_status, d.d_sell
        HAVING (:status IS NULL 
            OR (:status != 'REPORTED' OR COUNT(DISTINCT n.ng_idx) > 0))
        ORDER BY p.pd_create DESC
        """, nativeQuery = true)
    Page<AdminProductProjection> findAdminProducts(
            @Param("status") String status,
            Pageable pageable
    );
}
