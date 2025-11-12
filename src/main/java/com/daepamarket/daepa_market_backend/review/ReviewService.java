// src/main/java/com/daepamarket/daepa_market_backend/review/ReviewService.java
package com.daepamarket.daepa_market_backend.review;

import com.daepamarket.daepa_market_backend.domain.deal.DealEntity;
import com.daepamarket.daepa_market_backend.domain.deal.DealRepository;
import com.daepamarket.daepa_market_backend.domain.review.ReviewEntity;
import com.daepamarket.daepa_market_backend.domain.review.ReviewRepository;
import com.daepamarket.daepa_market_backend.domain.user.UserEntity;
import com.daepamarket.daepa_market_backend.domain.user.UserRepository;
import com.daepamarket.daepa_market_backend.review.dto.ReviewCreateRequest;
import com.daepamarket.daepa_market_backend.review.dto.ReviewUpdateRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final UserRepository userRepository;
    private final DealRepository dealRepository;

    @Transactional
    public Long createReview(Long writerIdx, ReviewCreateRequest dto) {

        // 1) 필수값 체크
        if (dto.getDIdx() == null) {
            throw new IllegalArgumentException("거래 번호(dIdx)가 없습니다.");
        }

        // 2) 작성자 조회
        UserEntity writer = userRepository.findById(writerIdx)
                .orElseThrow(() -> new IllegalArgumentException("작성자를 찾을 수 없습니다."));

        // 3) 거래 조회
        DealEntity deal = dealRepository.findById(dto.getDIdx())
                .orElseThrow(() -> new IllegalArgumentException("거래를 찾을 수 없습니다."));

        // 4) reType 기본값 지정
        String reType = dto.getReType();
        if (reType == null || reType.isBlank()) {
            reType = "BUYER";
        }

        // ✅ 디버그
        System.out.println("--------------------------------------------------");
        System.out.println("📘 [리뷰 생성 요청]");
        System.out.println("   dIdx        = " + dto.getDIdx());
        System.out.println("   writerIdx   = " + writerIdx);
        System.out.println("   reType      = " + reType);
        System.out.println("   reStar      = " + dto.getReStar());
        System.out.println("   reContent   = " + dto.getReContent());
        System.out.println("--------------------------------------------------");

        // 5) 중복 리뷰 체크
        boolean already = reviewRepository
                .existsByDeal_dIdxAndWriter_uIdxAndReType(dto.getDIdx(), writerIdx, reType);

        System.out.println("📗 [중복 리뷰 검사 결과] already = " + already);
        System.out.println("--------------------------------------------------");

        if (already) {
            throw new IllegalStateException("이미 이 거래에 대해 리뷰를 작성하셨습니다.");
        }

        // 6) 실제 저장
        ReviewEntity review = ReviewEntity.builder()
                .deal(deal)
                .writer(writer)
                .reContent(dto.getReContent())
                .reStar(dto.getReStar() != null ? dto.getReStar() : 0)
                .reCreate(LocalDateTime.now())
                .reUpdate(LocalDateTime.now())
                .reType(reType)
                .build();

        ReviewEntity saved = reviewRepository.save(review);

        System.out.println("✅ [리뷰 저장 완료] reIdx = " + saved.getReIdx());
        System.out.println("--------------------------------------------------");

        return saved.getReIdx();
    }

    /**
     * ✅ 프론트에서 "이미 썼냐" 먼저 확인할 때 쓰는 메서드
     */
    @Transactional(readOnly = true)
    public boolean existsReview(Long writerIdx, Long dIdx, String reType) {
        return reviewRepository.existsByDeal_dIdxAndWriter_uIdxAndReType(dIdx, writerIdx, reType);
    }

    @Transactional
    public void updateReview(Long writerIdx, Long reIdx, ReviewUpdateRequest dto) {
        ReviewEntity review = reviewRepository.findById(reIdx)
                .orElseThrow(() -> new IllegalArgumentException("리뷰를 찾을 수 없습니다."));

        // 작성자만 수정 가능
        if (!review.getWriter().getUIdx().equals(writerIdx)) {
            throw new IllegalStateException("본인이 작성한 후기만 수정할 수 있습니다.");
        }

        Integer star = dto.getReStar();
        if (star == null || star < 1 || star > 5) {
            throw new IllegalArgumentException("별점은 1~5 사이여야 합니다.");
        }
        String content = dto.getReContent();
        if (content == null) content = "";
        if (content.length() > 500) {
            throw new IllegalArgumentException("후기 내용은 500자를 초과할 수 없습니다.");
        }

        review.setReStar(star);
        review.setReContent(content);
        // @PreUpdate로 reUpdate가 자동 변경됨
        // JPA flush 시점에 업데이트
    }
}
