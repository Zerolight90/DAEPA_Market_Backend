package com.daepamarket.daepa_market_backend.domain.delivery;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "delivery")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DeliveryEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "dv_idx")
    private Long dvIdx;

    @Column(name = "loc_key")
    private Long locKey;

    @Column(name = "ck_idx")
    private Long ckIdx;

    /**
     * 0: 배송전
     * 1: 배송중
     * 2: 배송완료
     * 3: 반품 (검수결과:1)
     */
    @Column(name = "dv_status")
    private Long dvStatus;
}
