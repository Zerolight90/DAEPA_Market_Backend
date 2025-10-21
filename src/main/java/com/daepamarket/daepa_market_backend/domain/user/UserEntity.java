package com.daepamarket.daepa_market_backend.domain.user;

import jakarta.persistence.*;
import lombok.*;
import java.time.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@Entity
@Table(name = "user")
public class UserEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "u_idx")
    private Long uIdx;

    @Column(name = "u_id")
    private String uid;

    @Column(name = "u_name", length = 20)
    private String uName;

    @Column(name = "u_location", length = 20)
    private String uLocation;

    @Column(name = "u_profile", length = 250)
    private String uProfile;

    @Column(name = "u_manner")
    private Double uManner;

    @Column(name = "u_date")
    private LocalDateTime uDate;

    @Column(name = "u_pw", length = 200)
    private String uPw;

    @Column(name = "u_birth")
    private String uBirth;

    @Column(name = "u_gender", length = 10)
    private String uGender;

    @Column(name = "u_status")
    private Integer uStatus;

    @Column(name = "u_warn")
    private Integer uWarn;

    @Column(name = "u_accesstoken", length = 100)
    private String uAccessToken;

    @Column(name = "u_refreshtoken", length = 100)
    private String uRefreshToken;

    @Column(name = "u_jointype", length = 100)
    private String uJoinType;

    @Column(name = "u_nickname", length = 20)
    private String unickname;

    @Column(name = "u_agree", length = 20)
    private String uAgree;

    @Column(name = "u_phone", length = 20)
    private String uphone;

    @Column(name = "u_address", length = 200)
    private String uAddress;

    @Column(name = "u_location_detail", length = 200)
    private String uLocationDetail;
}
