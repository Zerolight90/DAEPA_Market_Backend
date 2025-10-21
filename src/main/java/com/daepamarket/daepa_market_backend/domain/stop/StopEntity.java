package com.daepamarket.daepa_market_backend.domain.stop;

import com.daepamarket.daepa_market_backend.domain.user.UserEntity;
import jakarta.persistence.*;
import lombok.*;
import java.time.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@Entity
@Table(name = "stop")
public class StopEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "stop_idx")
    private Long stopIdx;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "u_idx", nullable = false)
    private UserEntity user;

    @Column(name = "stop_date")
    private LocalDate stopDate;

    @Column(name = "stop_content", length = 250)
    private String stopContent;

    @Column(name = "stop_since")
    private LocalDate stopSince;
}
