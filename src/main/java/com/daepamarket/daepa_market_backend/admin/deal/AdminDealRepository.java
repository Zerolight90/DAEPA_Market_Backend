package com.daepamarket.daepa_market_backend.admin.deal;

import com.daepamarket.daepa_market_backend.domain.deal.DealEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface AdminDealRepository extends JpaRepository<DealEntity, Long> {

    // 관리자용: 구매자별 거래 조회
    @Query("SELECT d FROM DealEntity d JOIN FETCH d.product WHERE d.buyer.uIdx = :uIdx")
    List<DealEntity> findByBuyer_uIdx(@Param("uIdx") Long uIdx);

    // 관리자용: 판매자별 거래 조회
    @Query("SELECT d FROM DealEntity d JOIN FETCH d.product WHERE d.seller.uIdx = :uIdx")
    List<DealEntity> findBySeller_uIdx(@Param("uIdx") Long uIdx);

    // 관리자용: 일간 거래 건수 조회 (날짜 범위) - 총 금액, 판매자 수 포함
    @Query(value = """
        SELECT 
            DATE(d.d_edate) AS deal_date,
            COUNT(*) AS deal_count,
            COALESCE(SUM(d.agreed_price), 0) AS total_amount,
            COUNT(DISTINCT d.seller_idx2) AS seller_count
        FROM deal d
        WHERE d.d_edate >= :startDate 
          AND d.d_edate < :endDate
        GROUP BY DATE(d.d_edate)
        ORDER BY deal_date ASC
        """, nativeQuery = true)
    List<Object[]> findDailyDealCounts(@Param("startDate") java.sql.Timestamp startDate,
                                       @Param("endDate") java.sql.Timestamp endDate);
}

