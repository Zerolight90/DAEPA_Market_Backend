package com.daepamarket.daepa_market_backend.domain.delivery;

import com.daepamarket.daepa_market_backend.domain.deal.DealEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "delivery")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class deliveryEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "dv_idx")
    private Long dvIdx;

    // 배송지가 필요하면 그대로
    @Column(name = "loc_key")
    private Long locKey;

    @Column(name = "ck_idx")
    private Long ckIdx;

    @Column(name = "dv_status")
    private Integer dvStatus;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "d_idx", nullable = false)
    private DealEntity deal;
}
