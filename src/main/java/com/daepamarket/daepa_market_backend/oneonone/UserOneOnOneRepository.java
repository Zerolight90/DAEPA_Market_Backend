package com.daepamarket.daepa_market_backend.oneonone;

import com.daepamarket.daepa_market_backend.domain.oneonone.OneOnOneEntity;
import com.daepamarket.daepa_market_backend.domain.user.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UserOneOnOneRepository extends JpaRepository<OneOnOneEntity, Long> {

    // 마이페이지에서 내 문의 보기 이런 용도
    List<OneOnOneEntity> findByUserOrderByOoDateDesc(UserEntity user);
}
