// src/main/java/com/daepamarket/daepa_market_backend/domain/review/ReviewRepository.java
package com.daepamarket.daepa_market_backend.domain.review;

import com.daepamarket.daepa_market_backend.admin.review.AllReviewDTO;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ReviewRepository extends JpaRepository<ReviewEntity, Long> {

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
            r.reType,
            buyer.uIdx,
            seller.uIdx,
            r.writer.uIdx,
            r.writer.unickname
        )
        FROM ReviewEntity r
            JOIN r.deal d
            JOIN d.product p
            JOIN d.buyer buyer
            JOIN d.seller seller
        ORDER BY r.reCreate DESC
        """)
    List<AllReviewDTO> findAllReviewRows();

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
            r.reType,
            buyer.uIdx,
            seller.uIdx,
            r.writer.uIdx,
            r.writer.unickname
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

    /**
     * 이 거래(dIdx)에, 이 작성자(uIdx)가, 이 타입(reType)으로 이미 리뷰를 썼는지 확인
     */
    boolean existsByDeal_dIdxAndWriter_uIdxAndReType(Long dIdx,
                                                     Long uIdx,
                                                     String reType);
}
