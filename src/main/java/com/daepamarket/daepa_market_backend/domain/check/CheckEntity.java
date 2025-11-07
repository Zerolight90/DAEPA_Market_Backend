package com.daepamarket.daepa_market_backend.domain.check;

import jakarta.persistence.*;
import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@Entity
@Table(name = "`check`")
public class CheckEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ck_idx")
    private Long ckIdx;

    @Column(name = "ck_status")
    private Integer ckStatus;  // 0: 검수중, 1: 완료

    @Column(name = "ck_result")
    private Integer ckResult;  // 0: 불합격, 1: 합격

}
