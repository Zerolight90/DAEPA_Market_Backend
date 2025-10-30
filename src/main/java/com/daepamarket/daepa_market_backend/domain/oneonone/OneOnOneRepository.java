package com.daepamarket.daepa_market_backend.domain.oneonone;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface OneOnOneRepository extends JpaRepository<OneOnOneEntity, Long> {

    @Query("SELECT o FROM OneOnOneEntity o JOIN FETCH o.user ORDER BY o.ooDate DESC")
    List<OneOnOneEntity> findAllWithUser();
}
