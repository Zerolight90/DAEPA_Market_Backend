package com.daepamarket.daepa_market_backend.domain.deal;

import com.daepamarket.daepa_market_backend.domain.product.ProductEntity;
import com.daepamarket.daepa_market_backend.domain.user.UserEntity;
import jakarta.persistence.*;
import lombok.*;

import java.sql.Timestamp;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@Entity
@Table(name = "deal")
public class DealEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "d_idx")
    private Long dIdx;

    // deals(1:1) products
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pd_idx", unique = true, nullable = false)
    private ProductEntity product;

    // deals(N:1) users(구매자/판매자)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "buyer_idx", nullable = true)
    private UserEntity buyer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seller_idx2", nullable = false)
    private UserEntity seller;

    @Column(name = "d_deal", length = 20)
    private String dDeal;

    @Column(name = "d_status")
    private Long dStatus;

    @Column(name = "d_edate")
    private Timestamp dEdate;

    @Column(name = "agreed_price")
    private Long agreedPrice;

    @Column(name = "d_sell", length = 20)
    private String dSell;

    @Column(name = "d_buy", length = 20)
    private String dBuy;
}
