package com.daepamarket.daepa_market_backend.domain.getout;

import com.daepamarket.daepa_market_backend.domain.user.UserEntity;
import jakarta.persistence.*;
import lombok.*;
import java.time.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@Entity
@Table(name = "getout")
public class GetoutEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "go_idx")
    private Long goIdx;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "u_idx", nullable = false)
    private UserEntity user;

    @Column(name = "go_status", length = 200)
    private String goStatus;

    @Column(name = "go_outdate")
    private LocalDate goOutdata;
}
