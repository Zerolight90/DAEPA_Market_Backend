package com.daepamarket.daepa_market_backend.domain.pay;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface PayRepository extends JpaRepository<PayEntity, Long> {

    /**
     * 특정 사용자의 현재 총 잔액을 계산합니다.
     * pay 테이블에 있는 해당 유저의 모든 pa_price를 합산합니다.
     * @param userId
     * @return 현재 잔액 (기록이 없으면 null일 수 있으므로 주의)
     */
    
    @Query("SELECT p.paNprice FROM PayEntity p WHERE p.user.uIdx = :userId")
    Long calculateTotalBalanceByUserId(@Param("userId") Long userId);
}