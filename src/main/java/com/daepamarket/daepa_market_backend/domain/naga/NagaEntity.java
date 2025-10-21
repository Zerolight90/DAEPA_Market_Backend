package com.daepamarket.daepa_market_backend.domain.naga;

import jakarta.persistence.*;
import lombok.*;
import java.time.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@Entity
@Table(name = "naga")
public class NagaEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ng_idx")
    private Long ngIdx;

    @Column(name = "s_idx")
    private Long sIdx;

    @Column(name = "b_idx2")
    private Long bIdx2;

    @Column(name = "ng_data")
    private LocalDate ngDate;

    @Column(name = "ng_content", length = 400)
    private String ngContent;

    @Column(name = "ng_status")
    private Integer ngStatus;
}
