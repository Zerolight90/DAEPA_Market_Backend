package com.daepamarket.daepa_market_backend.domain.Category;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@Entity
@Table(name = "ct_low")
public class CtLowEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "low_idx")
    private Long lowIdx;

    @Column(name = "low_ct", length = 30)
    private String lowCt;

    // 중위 카테고리 연결 (N:1)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "middle_idx")
    @JsonIgnore
    private CtMiddleEntity middle;
}
