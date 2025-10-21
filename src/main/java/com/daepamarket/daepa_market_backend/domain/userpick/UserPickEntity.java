package com.daepamarket.daepa_market_backend.domain.userpick;

import java.math.BigInteger;

import com.daepamarket.daepa_market_backend.domain.Category.CtLowEntity;
import com.daepamarket.daepa_market_backend.domain.Category.CtMiddleEntity;
import com.daepamarket.daepa_market_backend.domain.Category.CtUpperEntity;
import com.daepamarket.daepa_market_backend.domain.user.UserEntity;
import jakarta.persistence.*;
import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@Entity
@Table(name = "userpick")
public class UserPickEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "up_idx", length = 255)
    private Long upIdx;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "u_idx", nullable = false)
    private UserEntity user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ct_low", nullable = false)
    private CtLowEntity ctLow;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "middle_idx", nullable = false)
    private CtMiddleEntity ctMiddle;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "upper_idx", nullable = false)
    private CtUpperEntity ctUpper;
    
    @Column(name = "up_low_cost")
    private int minPrice;

    @Column(name = "up_high_cost")
    private int maxPrice;
}
