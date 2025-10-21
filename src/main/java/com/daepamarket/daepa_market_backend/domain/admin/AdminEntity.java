package com.daepamarket.daepa_market_backend.domain.admin;

import jakarta.persistence.*;
import lombok.*;
import java.time.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "admin")
public class AdminEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ad_idx")
    private Long adIdx;

    @Column(name = "ad_id", length = 50)
    private String adId;

    @Column(name = "ad_pw", length = 100)
    private String adPw;

    @Column(name = "ad_name", length = 30)
    private String adName;

    @Column(name = "ad_status")
    private Integer adStatus;

    @Column(name = "ad_birth")
    private LocalDate adBirth;

    @Column(name = "ad_nick", length = 50)
    private String adNick;
}
