package com.daepamarket.daepa_market_backend.domain.notice;

import com.daepamarket.daepa_market_backend.domain.admin.AdminEntity;
import jakarta.persistence.*;
import lombok.*;
import java.time.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@Entity
@Table(name = "notice")
public class NoticeEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "n_idx")
    private Long nIdx;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ad_idx", nullable = false)
    private AdminEntity admin;

    @Column(name = "n_subject", length = 50)
    private String nSubject;

    @Lob
    @Column(name = "n_content")
    private String nContent;

    @Column(name = "n_img", length = 255)
    private String nImg;

    @Column(name = "n_date")
    private LocalDate nDate;

    @Column(name = "n_ip", length = 30)
    private String nIp;

    @Column(name = "n_category")
    private Byte nCategory;

    @Column(name = "n_fix")
    private Byte nFix;
}
