package com.daepamarket.daepa_market_backend.jwt;
import io.jsonwebtoken.Jwts;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.web.server.ResponseStatusException;

import javax.crypto.SecretKey;

@Slf4j
@Component

public class JwtProvider {
    private final JwtProps props;

    public JwtProvider(JwtProps props) {
        this.props = props;
    }

    //AccessToken 생성
    public String createAccessToken(String u_id, String u_jointype){
        Instant now = Instant.now();

        //엑세스 토큰 만료 시간 계산
        Instant exp = now.plus(props.getAccessExpMin(), ChronoUnit.MINUTES);

        return Jwts.builder()
                .issuer(props.getIssuer())
                .subject(String.valueOf(u_id))
                .claim("jointype:",u_jointype)
                .issuedAt(Date.from(now)) //토큰 발급 시간
                .expiration(Date.from(exp)) //토큰 만료 시간
                .signWith(getKey(), Jwts.SIG.HS256)
                .compact(); //토큰 생성
    }

    //RefreshToken 생성
    public String createRefreshToken(String u_idx){
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

    //토큰 유효성 검사 후 객체로 변환
    public Jws<Claims> parse(String token){
        return Jwts.parser().verifyWith(getKey()).build().parseSignedClaims(token);
    }

    //토큰에서 아이디 추출
    public String getUid(String accessToken){
        try {
            return parse(accessToken).getPayload().getSubject();
        } catch (io.jsonwebtoken.ExpiredJwtException e) {
            // 토큰이 만료되었을 때
            log.error("Expired JWT: {}", accessToken);
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "토큰이 만료되었습니다. 다시 로그인해 주세요.");
        } catch (Exception e) {
            // 그 외 토큰 파싱 오류 (서명 오류 등)
            log.error("Invalid JWT: {}", accessToken);
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "유효하지 않은 토큰입니다.");
        }
    }

    //토큰 만료 여부 확인 true -> 만료, false -> 유효
    public boolean isExpired(String token) {
        return parse(token).getPayload().getExpiration().before(new Date());
    }

    //비밀키
    private SecretKey getKey(){
        return Keys.hmacShaKeyFor(props.getSecret().getBytes(StandardCharsets.UTF_8));
    }
}
