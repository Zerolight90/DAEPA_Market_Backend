package com.daepamarket.daepa_market_backend.domain.oneonone;

import com.daepamarket.daepa_market_backend.domain.user.UserEntity;
import jakarta.persistence.*;
import lombok.*;
import java.time.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@Entity
@Table(name = "1on1")
public class OneOnOneEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "oo_idx")
    private Long ooIdx;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "u_idx", nullable = false)
    private UserEntity user;

    @Column(name = "oo_status")
    private Integer ooStatus;

    @Column(name = "oo_title", length = 50)
    private String ooTitle;

    @Column(name = "oo_content", length = 250)
    private String ooContent;

    @Column(name = "oo_photo", length = 250)
    private String ooPhoto;

    @Column(name = "oo_date")
    private LocalDate ooDate;

    @Column(name = "oo_re", length = 250)
    private String ooRe;
}