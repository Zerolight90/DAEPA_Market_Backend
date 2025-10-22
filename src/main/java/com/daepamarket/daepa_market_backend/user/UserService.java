package com.daepamarket.daepa_market_backend.user;

import com.daepamarket.daepa_market_backend.domain.admin.UserResponseDTO;
import com.daepamarket.daepa_market_backend.domain.user.UserSignUpDTO;
import com.daepamarket.daepa_market_backend.domain.user.UserEntity;
import com.daepamarket.daepa_market_backend.domain.user.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;

    public boolean existsByuId(String uId) {
        return userRepository.existsByUid(uId);
    }

    public boolean existsByuNickname(String uNickname) {
        return userRepository.existsByUnickname(uNickname);
    }

    public boolean existsByuPhone(String uPhone) {
        return userRepository.existsByUphone(uPhone);
    }

    @Transactional
    public Long signup(UserSignUpDTO rep) {
        //중복 검사
        if(userRepository.existsByUid(rep.getU_id())){
            throw new IllegalStateException("이미 존재하는 이메일입니다.");
        }
        if(userRepository.existsByUnickname(rep.getU_nickname())){
            throw new IllegalStateException("이미 존재하는 별명입니다.");
        }
        if(userRepository.existsByUphone(rep.getU_phone())){
            throw new IllegalStateException("이미 존재하는 전화번호입니다.");
        }

//        String agree;
//
//        if("true".equalsIgnoreCase(rep.getU_agree())){
//            agree = "1";
//        }
//        else {
//            agree = "0";
//        }

        LocalDateTime now = LocalDateTime.now();

        UserEntity user = userRepository.save(UserEntity.builder()
                .uid(rep.getU_id())
                .uPw(rep.getU_pw())
                .uName(rep.getU_name())
                .unickname(rep.getU_nickname())
                .uphone(rep.getU_phone())
                .uAddress(rep.getU_address())
                .uLocation(rep.getU_location())
                .uLocationDetail(rep.getU_location_detail())
                .uBirth(rep.getU_birth())
                .uGender(rep.getU_gender())
                .uDate(now)
                .uAgree(rep.getU_agree())
                .uJoinType("로컬")
                .uStatus(1)
                .uWarn(0)
                .uManner(20.0)
                .build()
        );

        return user.getUIdx();
    }

    public UserEntity findUserById (Long user){
        return userRepository.findById(user)
                .orElseThrow(() -> new RuntimeException("User Not Found: " + user));
    }

    /* 관리자용 전체 사용자 조회 */
    public List<UserResponseDTO> findAllUsers() {
        return userRepository.findAll()
                .stream()
                .map(UserResponseDTO::of)
                .toList();
    }





}
