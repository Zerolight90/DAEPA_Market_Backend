package com.daepamarket.daepa_market_backend.domain.pay;

import com.daepamarket.daepa_market_backend.domain.deal.DealEntity;
import com.daepamarket.daepa_market_backend.domain.user.UserEntity;
import jakarta.persistence.*;
import lombok.*;
import java.time.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@Entity
@Table(name = "pay")
public class PayEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "pa_idx")
    private Long paIdx;

    @Column(name = "pa_date")
    private LocalDate paDate;

    @Column(name = "pa_price")
    private Long paPrice;

    @Column(name = "pa_nprice")
    private Long paNprice;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "u_idx", nullable = false)
    private UserEntity user;

    // @ManyToOne(fetch = FetchType.LAZY)
    // @JoinColumn(name = "`d_idx`", nullable = false)
    // private DealEntity deal;

    @Column(name = "pa_point")
    private Integer paPoint;
}
