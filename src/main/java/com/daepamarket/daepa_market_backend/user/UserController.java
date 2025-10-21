package com.daepamarket.daepa_market_backend.user;

import com.daepamarket.daepa_market_backend.domain.user.UserRepository;
import com.daepamarket.daepa_market_backend.domain.user.UserSignUpDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

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


}
