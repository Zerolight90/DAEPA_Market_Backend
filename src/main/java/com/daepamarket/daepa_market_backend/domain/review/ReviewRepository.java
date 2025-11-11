package com.daepamarket.daepa_market_backend.domain.review;

import com.daepamarket.daepa_market_backend.admin.review.AllReviewDTO;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ReviewRepository extends JpaRepository<ReviewEntity, Long> {

    /**
     * 관리자 전체 리뷰
     */
    @Query("""
        SELECT new com.daepamarket.daepa_market_backend.admin.review.AllReviewDTO(
            CASE WHEN r.reType = 'SELLER'
                 THEN CONCAT('S-', r.reIdx)
                 ELSE CONCAT('B-', r.reIdx)
            END,
            r.reIdx,
            p.pdTitle,
            buyer.unickname,
            seller.unickname,
            r.reStar,
            r.reContent,
            r.reCreate,
            r.reType
        )
        FROM ReviewEntity r
            JOIN r.deal d
            JOIN d.product p
            JOIN d.buyer buyer
            JOIN d.seller seller
        ORDER BY r.reCreate DESC
        """)
    List<AllReviewDTO> findAllReviewRows();


    /**
     * 특정 유저가 관련된 후기만 (구매자 or 판매자)
     */
    @Query("""
        SELECT new com.daepamarket.daepa_market_backend.admin.review.AllReviewDTO(
            CASE WHEN r.reType = 'SELLER'
                 THEN CONCAT('S-', r.reIdx)
                 ELSE CONCAT('B-', r.reIdx)
            END,
            r.reIdx,
            p.pdTitle,
            buyer.unickname,
            seller.unickname,
            r.reStar,
            r.reContent,
            r.reCreate,
            r.reType
        )
        FROM ReviewEntity r
            JOIN r.deal d
            JOIN d.product p
            JOIN d.buyer buyer
            JOIN d.seller seller
        WHERE buyer.uIdx = :userId
           OR seller.uIdx = :userId
        ORDER BY r.reCreate DESC
        """)
    List<AllReviewDTO> findReviewRowsByTargetUser(@Param("userId") Long userId);
}
