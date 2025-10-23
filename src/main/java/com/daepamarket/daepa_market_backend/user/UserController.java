package com.daepamarket.daepa_market_backend.user;

import com.daepamarket.daepa_market_backend.domain.user.UserEntity;
import com.daepamarket.daepa_market_backend.domain.user.UserLoginDTO;
import com.daepamarket.daepa_market_backend.domain.user.UserRepository;
import com.daepamarket.daepa_market_backend.domain.user.UserSignUpDTO;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/sing")
public class UserController {
    private final UserService userService;


    @PostMapping("/join/signup")
    public String signup(@RequestBody UserSignUpDTO userSigndto) throws Exception{
        userService.signup(userSigndto);
        return "회원가입 성공";
    }

    //아이디 중복 확인
    @GetMapping("/join/check_id")
    public boolean checkuId(@RequestParam("u_id") String uId) {
        return userService.existsByuId(uId);
    }

    //별명 중복 확인
    @GetMapping("/join/check_nickname")
    public boolean existsByuNickname(@RequestParam("u_nickname") String uNickname) {
        return userService.existsByuNickname(uNickname);
    }

    //전화번호 중복 확인
    @GetMapping("/join/check_phone")
    public boolean existsByuPhone(@RequestParam("u_phone") String uPhone) {
        return userService.existsByuPhone(uPhone);
    }

    // 로그인
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody UserLoginDTO dto) {
        return userService.login(dto);
    }

    // 토큰 재발급
    @PostMapping("/refresh")
    public ResponseEntity<?> refresh(HttpServletRequest request) {
        return userService.refresh(request);
    }

    // 로그아웃
    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpServletRequest request) {
        return userService.logout(request);
    }

    //현재 로그인 사용자 정보 조회
    @GetMapping("/me")
    public ResponseEntity<?> getMe(HttpServletRequest request) {
        return userService.getMe(request);
    }

    @GetMapping("/find_id")
    public Optional<UserEntity> find_id(@RequestParam("u_name") String uName,
                                        @RequestParam("u_phone") String uPhone) {
        return userService.findByUNameAndUphone(uName, uPhone);
    }






}
