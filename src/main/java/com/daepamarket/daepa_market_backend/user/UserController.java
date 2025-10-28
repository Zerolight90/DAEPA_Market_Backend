package com.daepamarket.daepa_market_backend.user;

import com.daepamarket.daepa_market_backend.domain.user.UserEntity;
import com.daepamarket.daepa_market_backend.domain.user.UserLoginDTO;
import com.daepamarket.daepa_market_backend.domain.user.UserRepository;
import com.daepamarket.daepa_market_backend.domain.user.UserSignUpDTO;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.text.DateFormat;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
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

    //아이디 찾기
    @PostMapping("/login/find_id")
    public ResponseEntity<?> find_id(@RequestBody Map<String, String> requestData) {
        String uName = requestData.get("u_name");
        String uPhone = requestData.get("u_phone");

        Optional<UserEntity> result = userService.findByUNameAndUphone(uName, uPhone);

        if(result.isPresent()) {
            UserEntity user = result.get();

            DateTimeFormatter format = DateTimeFormatter.ofPattern("yyyy-MM-dd");
            String formatdate = user.getUDate().format(format);

            Map<String, Object> map = new HashMap<>();
            map.put("uId", user.getUid());
            map.put("uDate", formatdate);

            return ResponseEntity.ok(map);
        }
        else {
            return ResponseEntity.status(404).body(Map.of("message", "일치하는 회원 정보가 없습니다."));
        }
    }

    //비밀번호 찾기
    @PostMapping("/login/find_password")
    public Optional<UserEntity> find_password(@RequestBody Map<String, String> requestData) {
        String uId = requestData.get("u_id");
        String uName = requestData.get("u_name");
        String uPhone = requestData.get("u_phone");

        return userService.findByUidAndUnameAndUphone(uId, uName, uPhone);

    }

    @PutMapping ("/login/find_password/reset")
    public ResponseEntity<Map<String, String>> reset_password(@RequestBody Map<String, String> requestData){
        String uId = requestData.get("u_id");
        String newPw = requestData.get("new_password");

        try{
            //성공했을 때
            userService.reset_password(uId, newPw);

            return ResponseEntity.ok(Map.of("message", "비밀번호가 성공적으로 변경되었습니다."));
        }
        //사용자를 찾지 못함
        catch (IllegalArgumentException e){
            return ResponseEntity.notFound().build();
        }
    }
}
