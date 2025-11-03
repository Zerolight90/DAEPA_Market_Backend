package com.daepamarket.daepa_market_backend.domain.user;

import org.apache.catalina.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.Optional;


@Repository
public interface UserRepository extends JpaRepository<UserEntity,Long> {
    //이메일, 별명, 전화번호 중복체크
    boolean existsByUid(String uid);
    boolean existsByUnickname(String uNickname);
    boolean existsByUphone(String uPhone);

    Optional<UserEntity> findByUid(String uid);

    Optional<UserEntity> findByUrefreshToken(String uRefreshToken);

    Optional<UserEntity> findByUnameAndUphone(String uname, String uphone);

    Optional<UserEntity> findByUidAndUnameAndUphone(String uid, String uname, String uphone);


    String uid(String uid);
}
