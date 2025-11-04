package com.daepamarket.daepa_market_backend.jwt;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;

@Slf4j
@Component
public class JwtProvider {

    private final JwtProps props;

    public JwtProvider(JwtProps props) {
        this.props = props;
    }

    // AccessToken 생성
    public String createAccessToken(String u_id, String u_jointype) {
        Instant now = Instant.now();
        Instant exp = now.plus(props.getAccessExpMin(), ChronoUnit.MINUTES);

        return Jwts.builder()
                .issuer(props.getIssuer())
                .subject(String.valueOf(u_id))
                .claim("jointype:", u_jointype)
                .issuedAt(Date.from(now))
                .expiration(Date.from(exp))
                .signWith(getKey(), Jwts.SIG.HS256)
                .compact();
    }

    // RefreshToken 생성
    public String createRefreshToken(String u_idx) {
        Instant now = Instant.now();
        Instant exp = now.plus(props.getRefreshExpDays(), ChronoUnit.DAYS);

        return Jwts.builder()
                .claim("userIdx", u_idx)
                .issuer(props.getIssuer())
                .subject("refresh")
                .issuedAt(Date.from(now))
                .expiration(Date.from(exp))
                .signWith(getKey(), Jwts.SIG.HS256)
                .compact();
    }

    // 공통 파서
    public Jws<Claims> parse(String token) {
        return Jwts.parser()
                .verifyWith(getKey())
                .build()
                .parseSignedClaims(token);
    }

    /**
     * 토큰에서 uid(subject) 뽑기
     * 토큰이 만료/손상되면 null만 리턴 (예외 던지지 않음)
     */
    public String getUid(String accessToken) {
        try {
            return parse(accessToken).getPayload().getSubject();
        } catch (ExpiredJwtException e) {
            log.warn("getUid: expired token");
            return null;
        } catch (JwtException e) {
            log.warn("getUid: invalid token {}", e.getMessage());
            return null;
        } catch (Exception e) {
            log.warn("getUid: error {}", e.getMessage());
            return null;
        }
    }

    /**
     * 토큰 만료 여부
     * - 파싱 실패해도 true로만 돌려주고 예외는 던지지 않는다
     */
    public boolean isExpired(String token) {
        try {
            Date exp = parse(token).getPayload().getExpiration();
            return exp.before(new Date());
        } catch (ExpiredJwtException e) {
            return true;
        } catch (JwtException e) {
            log.warn("isExpired: invalid token {}", e.getMessage());
            return true;
        } catch (Exception e) {
            log.warn("isExpired: error {}", e.getMessage());
            return true;
        }
    }

    // 비밀키
    private SecretKey getKey() {
        return Keys.hmacShaKeyFor(props.getSecret().getBytes(StandardCharsets.UTF_8));
    }
}
