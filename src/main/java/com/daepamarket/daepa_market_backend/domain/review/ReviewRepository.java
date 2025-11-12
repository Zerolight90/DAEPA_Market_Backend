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

    // ===== 관리자 조회용 =====
    @Query("""
        SELECT new com.daepamarket.daepa_market_backend.admin.review.AllReviewDTO(
            CASE WHEN r.reType = 'SELLER'
                 THEN CONCAT('S-', r.reIdx)
                 ELSE CONCAT('B-', r.reIdx)
            END,
            r.reIdx,
            p.pdTitle,
            COALESCE(buyer.unickname, ''),
            seller.unickname,
            r.reStar,
            r.reContent,
            r.reCreate,
            r.reType,
            buyer.uIdx,
            seller.uIdx,
            r.writer.uIdx,
            r.writer.unickname,
            d.dEdate
        )
        FROM ReviewEntity r
            JOIN r.deal d
            JOIN d.product p
            LEFT JOIN d.buyer buyer
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
            COALESCE(buyer.unickname, ''),
            seller.unickname,
            r.reStar,
            r.reContent,
            r.reCreate,
            r.reType,
            buyer.uIdx,
            seller.uIdx,
            r.writer.uIdx,
            r.writer.unickname,
            d.dEdate
        )
        FROM ReviewEntity r
            JOIN r.deal d
            JOIN d.product p
            LEFT JOIN d.buyer buyer
            JOIN d.seller seller
        WHERE (buyer.uIdx = :userId OR seller.uIdx = :userId OR r.writer.uIdx = :userId)
        ORDER BY r.reCreate DESC
    """)
    List<AllReviewDTO> findReviewRowsByTargetUser(@Param("userId") Long userId);

    // ===== 중복 체크 =====
    boolean existsByDeal_dIdxAndWriter_uIdxAndReType(Long dIdx, Long uIdx, String reType);

    // ===== 받은 후기 (내가 대상) =====
    @Query("""
        SELECT new com.daepamarket.daepa_market_backend.review.dto.MyReviewRow(
            r.reIdx,
            r.reType,
            r.reStar,
            r.reContent,
            r.reUpdate,
            d.dIdx,
            p.pdTitle,
            p.pdThumb,
            writer.unickname
        )
        FROM ReviewEntity r
            JOIN r.deal d
            JOIN d.product p
            JOIN r.writer writer
            JOIN d.buyer buyer
            JOIN d.seller seller
        WHERE (r.reType = 'BUYER'  AND seller.uIdx = :uid)
           OR (r.reType = 'SELLER' AND buyer.uIdx  = :uid)
        ORDER BY r.reUpdate DESC
    """)
    Page<MyReviewRow> pageReceivedByUser(@Param("uid") Long uid, Pageable pageable);

    // ===== 작성한 후기 (내가 작성자) =====
    @Query("""
        SELECT new com.daepamarket.daepa_market_backend.review.dto.MyReviewRow(
            r.reIdx,
            r.reType,
            r.reStar,
            r.reContent,
            r.reUpdate,
            d.dIdx,
            p.pdTitle,
            p.pdThumb,
            writer.unickname
        )
        FROM ReviewEntity r
            JOIN r.deal d
            JOIN d.product p
            JOIN r.writer writer
        WHERE writer.uIdx = :uid
        ORDER BY r.reUpdate DESC
    """)
    Page<MyReviewRow> pageWrittenByUser(@Param("uid") Long uid, Pageable pageable);

    // ===== 특정 유저(판매자/구매자)가 '받은' 후기 (공개용) =====
    @Query("""
        SELECT new com.daepamarket.daepa_market_backend.review.dto.MyReviewRow(
            r.reIdx,
            r.reType,
            r.reStar,
            r.reContent,
            r.reUpdate,
            d.dIdx,
            p.pdTitle,
            p.pdThumb,
            writer.unickname
        )
        FROM ReviewEntity r
            JOIN r.deal d
            JOIN d.product p
            JOIN r.writer writer
            JOIN d.buyer buyer
            JOIN d.seller seller
        WHERE (r.reType = 'BUYER'  AND seller.uIdx = :targetUserId)
           OR (r.reType = 'SELLER' AND buyer.uIdx  = :targetUserId)
        ORDER BY r.reUpdate DESC
    """)
    Page<MyReviewRow> pageReceivedByTargetUser(@Param("targetUserId") Long targetUserId, Pageable pageable);
}
