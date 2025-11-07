package com.daepamarket.daepa_market_backend.domain.oneonone;

import com.daepamarket.daepa_market_backend.domain.user.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface OneOnOneRepository extends JpaRepository<OneOnOneEntity, Long> {

    @Query("SELECT o FROM OneOnOneEntity o JOIN FETCH o.user ORDER BY o.ooDate DESC")
    List<OneOnOneEntity> findAllWithUser();

    // 상세 조회 (LazyInitializationException 해결)
    @Query("""
        SELECT o FROM OneOnOneEntity o
        JOIN FETCH o.user
        WHERE o.ooIdx = :id
    """)
    Optional<OneOnOneEntity> findByIdWithUser(@Param("id") Long id);

    // 사용자별 문의 내역 조회
    List<OneOnOneEntity> findByUserOrderByOoDateDesc(UserEntity user);
}
