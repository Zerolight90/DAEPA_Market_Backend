package com.daepamarket.daepa_market_backend.domain.manner;

import com.daepamarket.daepa_market_backend.domain.naga.NagaEntity;
import com.daepamarket.daepa_market_backend.domain.user.UserEntity;
import jakarta.persistence.*;
import lombok.*;
import java.time.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@Entity
@Table(name = "manner")
public class MannerEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "m_idx")
    private Long m_idx;

    @Column(name = "d_idx")
    private Long dIdx;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "u_idx", nullable = false)
    private UserEntity user;

    @Column(name = "m_score")
    private Integer mScore;

    @Column(name = "m_change")
    private Long mChange;

    @Column(name = "m_update")
    private LocalDateTime mUpdate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ng_idx", nullable = false)
    private NagaEntity naga;
}
