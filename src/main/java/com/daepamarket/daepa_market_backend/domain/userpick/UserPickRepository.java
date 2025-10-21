package com.daepamarket.daepa_market_backend.domain.userpick;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.daepamarket.daepa_market_backend.domain.user.UserEntity;


@Repository
public interface UserPickRepository extends JpaRepository<UserPickEntity, Long> {

    // @Query("SELECT ALL FROM UserPickEntity WHERE user = '김토스'")
    List<UserPickEntity> findByUser(UserEntity user);

}
