package com.daepamarket.daepa_market_backend.admin.delivery;

import com.daepamarket.daepa_market_backend.domain.delivery.DeliveryEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface AdminDeliveryRepository extends JpaRepository<DeliveryEntity, Long> {
    @Query("SELECT d FROM DeliveryEntity d WHERE d.checkEntity.ckIdx = :ckIdx")
    Optional<DeliveryEntity> findByCheckCkIdx(@Param("ckIdx") Long ckIdx);

    /**
     * 배송 관리에서 조회할 항목을 반환합니다.
     * 조건:
     * 1. 거래 타입이 'DELIVERY' (배송)
     * 2. 검수 완료 (ck_status = 1)
     * 검수 결과(ck_result)가 합격(0)이든 불합격(1)이든 관계없이
     * 위 조건을 만족하는 모든 항목을 반환합니다.
     * 주의: 구매 상태(d_buy)는 배송을 위해 완료되면 안 되므로 조건에서 제외합니다.
     * 
     * 검수 관리 페이지와 동일한 기준(delivery 테이블 기준)으로 조회하되,
     * 검수 완료(ck_status = 1)된 항목만 필터링합니다.
     * 이렇게 하면 검수 관리에서 불러온 데이터 중 검수 완료된 모든 항목을 포함합니다.
     */
    @Query(value = """
        SELECT 
            dl.dv_idx,
            d.d_idx,
            p.pd_title,
            seller.u_name AS seller_name,
            COALESCE(buyer.u_name, '') AS buyer_name,
            COALESCE(loc.loc_address, '') AS loc_address,
            COALESCE(loc.loc_detail, '') AS loc_detail,
            dl.dv_status,
            d.d_deal,
            d.d_edate
        FROM delivery dl
        JOIN deal d ON dl.d_idx = d.d_idx
        JOIN product p ON d.pd_idx = p.pd_idx
        JOIN `user` seller ON d.seller_idx2 = seller.u_idx
        LEFT JOIN `user` buyer ON d.buyer_idx = buyer.u_idx
        LEFT JOIN location loc ON dl.loc_key = loc.loc_key
        JOIN `check` c ON dl.ck_idx = c.ck_idx
        WHERE d.d_deal = 'DELIVERY'
          AND c.ck_status = 1
        ORDER BY c.ck_idx DESC
        """, nativeQuery = true)
    List<Object[]> findAllWithDetails();
}
