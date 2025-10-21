package com.daepamarket.daepa_market_backend.domain.review;

import com.daepamarket.daepa_market_backend.domain.deal.DealEntity;
import com.daepamarket.daepa_market_backend.domain.user.UserEntity;
import jakarta.persistence.*;
import lombok.*;
import java.time.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@Entity
@Table(name = "buy_review")
public class BuyReviewEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "re_idx")
    private Long reIdx;

    // reviews(N:1) deals
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "d_idx")
    private DealEntity deal;

    // reviews(N:1) users(리뷰 작성자)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "u_idx", nullable = false)
    private UserEntity writer;

    @Column(name = "re_content", length = 500)
    private String reContent;

    @Column(name = "re_create")
    private LocalDateTime reCreate;

    @Column(name = "re_update")
    private LocalDateTime reUpdate;

    @Column(name = "re_star")
    private Long reStar;
}
