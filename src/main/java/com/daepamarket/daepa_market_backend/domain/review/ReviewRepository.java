// src/main/java/com/daepamarket/daepa_market_backend/domain/review/ReviewRepository.java
package com.daepamarket.daepa_market_backend.domain.review;

import com.daepamarket.daepa_market_backend.admin.review.AllReviewDTO;
import com.daepamarket.daepa_market_backend.review.dto.MyReviewRow;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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

    /**
     * 이 거래(dIdx)에, 이 작성자(uIdx)가, 이 타입(reType)으로 이미 리뷰를 썼는지 확인
     */
    boolean existsByDeal_dIdxAndWriter_uIdxAndReType(Long dIdx,
                                                     Long uIdx,
                                                     String reType);
    // === (신규) 받은 후기: 내가 대상인 후기 (BUYER → seller가 대상 / SELLER → buyer가 대상) ===
    @Query("""
        SELECT new com.daepamarket.daepa_market_backend.review.dto.MyReviewRow(
            r.reIdx,
            d.dIdx,
            p.pdTitle,
            p.pdThumb,
            w.unickname,
            r.reStar,
            r.reContent,
            r.reCreate,
            r.reUpdate,    
            r.reType
        )
        FROM ReviewEntity r
            JOIN r.deal d
            JOIN d.product p
            JOIN r.writer w
            JOIN d.buyer buyer
            JOIN d.seller seller
        WHERE (r.reType = 'BUYER' AND seller.uIdx = :uid)
           OR (r.reType = 'SELLER' AND buyer.uIdx = :uid)
        ORDER BY r.reCreate DESC
        """)
    Page<MyReviewRow> pageReceivedByUser(@Param("uid") Long uid, Pageable pageable);

    // === (신규) 작성한 후기: 내가 작성자 ===
    @Query("""
        SELECT new com.daepamarket.daepa_market_backend.review.dto.MyReviewRow(
           r.reIdx,
           d.dIdx,
           p.pdTitle,
           p.pdThumb,
           w.unickname,
           r.reStar,
           r.reContent,
           r.reCreate,
           r.reUpdate,    
           r.reType
        )
        FROM ReviewEntity r
            JOIN r.deal d
            JOIN d.product p
            JOIN r.writer w
        WHERE w.uIdx = :uid
        ORDER BY r.reCreate DESC
        """)
    Page<MyReviewRow> pageWrittenByUser(@Param("uid") Long uid, Pageable pageable);
}
