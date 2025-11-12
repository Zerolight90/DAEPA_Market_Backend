package com.daepamarket.daepa_market_backend.domain.review;

import com.daepamarket.daepa_market_backend.domain.deal.DealEntity;
import com.daepamarket.daepa_market_backend.domain.user.UserEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "review")
public class ReviewEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "re_idx")
    private Long reIdx;

    // 어떤 거래에 대한 후기인지
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "d_idx", nullable = false)
    private DealEntity deal;

    // 후기 작성자
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "u_idx", nullable = false)
    private UserEntity writer;

    @Column(name = "re_content", length = 500)
    private String reContent;

    @Column(name = "re_star")
    private Integer reStar;

    @Column(name = "re_create")
    private LocalDateTime reCreate;

    @Column(name = "re_update")
    private LocalDateTime reUpdate;

    /**
     * BUYER  : 구매자가 남긴 후기 (→ 보통 판매자에게 남기는 후기)
     * SELLER : 판매자가 남긴 후기 (→ 보통 구매자에게 남기는 후기)
     */
    @Column(name = "re_type", length = 10, nullable = false)
    private String reType; // "BUYER" or "SELLER"

    @PrePersist
    public void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        this.reCreate = now;
        this.reUpdate = now;
    }

    @PreUpdate
    public void onUpdate() {
        this.reUpdate = LocalDateTime.now();
    }
}
