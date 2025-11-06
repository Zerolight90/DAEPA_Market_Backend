package com.daepamarket.daepa_market_backend.domain.user;

import org.apache.catalina.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;
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

    @Query("SELECT u.unickname FROM UserEntity u WHERE u.uIdx = :id")
    String findNicknameById(@Param("id") Long id);

    @Modifying
    @Transactional
    @Query("UPDATE UserEntity u SET u.uStatus = :status WHERE u.uIdx = :userId")
    void updateUserStatus(@Param("userId") Long userId, @Param("status") int status);


    String uid(String uid);
}
