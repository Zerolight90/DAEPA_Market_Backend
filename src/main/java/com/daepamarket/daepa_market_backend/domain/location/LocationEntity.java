package com.daepamarket.daepa_market_backend.domain.location;

import com.daepamarket.daepa_market_backend.domain.user.UserEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "location")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LocationEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "loc_key")
    private Long locKey; // 주소 PK

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "u_idx", nullable = false)
    private UserEntity user; // 어떤 사용자의 주소인지 (FK)

    @Column(name = "loc_address", length = 100)
    private String locAddress; // 기본 주소 (시/구/동 등)

    @Column(name = "loc_detail", length = 100)
    private String locDetail; // 상세 주소 (호수 등)

    @Column(name = "loc_default", nullable = false)
    private boolean locDefault; // 기본 주소 여부

    @Column(name = "loc_code", length = 100)
    private String locCode; // 우편번호
}

