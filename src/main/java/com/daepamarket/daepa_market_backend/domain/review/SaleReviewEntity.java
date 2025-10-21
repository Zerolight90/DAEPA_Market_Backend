package com.daepamarket.daepa_market_backend.domain.review;

import com.daepamarket.daepa_market_backend.domain.deal.DealEntity;
import com.daepamarket.daepa_market_backend.domain.user.UserEntity;
import jakarta.persistence.*;
import lombok.*;
import java.time.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@Entity
@Table(name = "sale_review")
public class SaleReviewEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "sr_idx")
    private Long srIdx;

    // reviews(N:1) deals
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "d_idx")
    private DealEntity deal;

    // reviews(N:1) users(리뷰 작성자)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "u_idx", nullable = false)
    private UserEntity writer;

    @Column(name = "sr_content", length = 500)
    private String srContent;

    @Column(name = "sr_create")
    private LocalDateTime srCreate;

    @Column(name = "sr_update")
    private LocalDateTime srUpdate;

    @Column(name = "sr_star")
    private Long srStar;
}
