package com.daepamarket.daepa_market_backend.admin.review;

import com.daepamarket.daepa_market_backend.domain.review.BuyReviewEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;

public interface BuyReviewRepository extends JpaRepository<BuyReviewEntity, Long> {

    @Query("""
        SELECT new com.daepamarket.daepa_market_backend.admin.review.AllReviewDTO(
            CONCAT('B-', br.reIdx),
            br.reIdx,
            p.pdTitle,
            buyer.unickname,
            seller.unickname,
            br.reStar,
            br.reContent,
            br.reCreate,
            'BUY'
        )
        FROM BuyReviewEntity br
        JOIN br.deal d
        JOIN d.product p
        JOIN br.writer buyer
        JOIN d.seller seller
        ORDER BY br.reCreate DESC
    """)
    List<AllReviewDTO> findAllBuyReviewRows();
}
