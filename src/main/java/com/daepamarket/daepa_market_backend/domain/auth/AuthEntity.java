package com.daepamarket.daepa_market_backend.domain.auth;
import jakarta.persistence.*;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "auth")
public class AuthEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "auth_idx")
    private Long authIdx;

    @Column(name = "auth_code", nullable = false, length = 16)
    private String authCode;

    @Column(name = "auth_email", nullable = false, length = 255)
    private String authEmail;
}
