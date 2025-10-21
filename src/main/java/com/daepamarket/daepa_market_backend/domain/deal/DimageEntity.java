package com.daepamarket.daepa_market_backend.domain.deal;

import jakarta.persistence.*;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "dimage")
public class DimageEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "di_idx")
    private Long diIdx;  // 거래이미지 기본키

    // 거래기본키 (Deal과 N:1 관계)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "d_idx", nullable = false)
    private DealEntity deal;

    @Column(name = "di_url", length = 250)
    private String diUrl; // 거래이미지 url
}
