package com.daepamarket.daepa_market_backend.admin.review;

import com.daepamarket.daepa_market_backend.domain.review.SaleReviewEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface SaleReviewRepository extends JpaRepository<SaleReviewEntity, Long> {

    // 판매자별 후기 (사용자 상세 페이지)
    @Query("""
        SELECT new com.daepamarket.daepa_market_backend.admin.review.SaleReviewDTO(
            sr.srIdx,
            u.unickname,
            sr.srStar,
            sr.srContent,
            sr.srCreate,
            p.pdTitle
    )
    FROM SaleReviewEntity sr
    JOIN sr.deal d
    JOIN d.product p
    JOIN sr.writer u
    WHERE d.seller.uIdx = :sellerId
    ORDER BY sr.srCreate DESC
""")
    List<SaleReviewDTO> findSaleReviewsBySeller(@Param("sellerId") Long sellerId);

    // 전체 후기 목록 (관리자 테이블용)
    @Query("""
        SELECT new com.daepamarket.daepa_market_backend.admin.review.AllReviewDTO(
            CONCAT('S-', sr.srIdx),
            sr.srIdx,
            p.pdTitle,
            buyer.unickname,
            seller.unickname,
            sr.srStar,
            sr.srContent,
            sr.srCreate,
            'SELL'
        )
        FROM SaleReviewEntity sr
        JOIN sr.deal d
        JOIN d.product p
        JOIN sr.writer buyer
        JOIN d.seller seller
        ORDER BY sr.srCreate DESC
    """)
    List<AllReviewDTO> findAllSaleReviewRows();
}
