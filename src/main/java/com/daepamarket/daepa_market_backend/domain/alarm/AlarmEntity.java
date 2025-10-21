package com.daepamarket.daepa_market_backend.domain.alarm;

import com.daepamarket.daepa_market_backend.domain.user.UserEntity;
import com.daepamarket.daepa_market_backend.domain.product.ProductEntity;
import jakarta.persistence.*;
import lombok.*;
import java.time.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@Entity
@Table(name = "alarm")
public class AlarmEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "al_idx")
    private Long alIdx;

    // alarms(N:1) users
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "u_idx", nullable = false)
    private UserEntity user;

    // alarms(N:1) products (nullable)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pd_idx")
    private ProductEntity product;

    @Column(name = "al_type", length = 50)
    private String alType;

    @Column(name = "al_read")
    private Boolean alRead;

    @Column(name = "al_del")
    private Boolean alDel;

    @Column(name = "al_create")
    private LocalDateTime alCreate;
}
