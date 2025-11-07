package com.daepamarket.daepa_market_backend.domain.delivery;

import com.daepamarket.daepa_market_backend.domain.check.CheckEntity;
import com.daepamarket.daepa_market_backend.domain.deal.DealEntity;
import com.daepamarket.daepa_market_backend.domain.location.LocationEntity;
import jakarta.persistence.*;
import lombok.*;

import java.sql.Timestamp;

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
    private Long dvIdx;   // 배송 키 (PK)

    // ---------------------- 관계 매핑 ----------------------

    // 거래 테이블 (Deal) 참조
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "d_idx", referencedColumnName = "d_idx",
            foreignKey = @ForeignKey(name = "fk_delivery_deal"))
    private DealEntity deal;

    // 주소 테이블 (Location) 참조
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "loc_key", referencedColumnName = "loc_key",
            foreignKey = @ForeignKey(name = "fk_delivery_location"))
    private LocationEntity location;

    // 검사 테이블 (Check) 참조
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ck_idx", referencedColumnName = "ck_idx",
            foreignKey = @ForeignKey(name = "fk_delivery_check"))
    private CheckEntity checkEntity;

    // 배송 상태
    @Column(name = "dv_status")
    private Integer dvStatus;

    @Column(name = "dv_date")
    private Timestamp dv_date;

    // ---------------------- 헬퍼 메서드 ----------------------
    public void updateStatus(Integer newStatus) {
        this.dvStatus = newStatus;
    }
}
