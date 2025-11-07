package com.daepamarket.daepa_market_backend.domain.check;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;

public interface CheckRepository extends JpaRepository<CheckEntity, Long> {

    @Query(value = """
        SELECT
            c.ck_idx AS ckIdx,
            d.d_idx AS dIdx,
            p.pd_title AS productName,
            u.u_name AS sellerName,
            d.d_deal AS tradeType,
            c.ck_status AS ckStatus,
            c.ck_result AS ckResult
        FROM delivery dl
        JOIN deal d ON dl.d_idx = d.d_idx
        JOIN product p ON d.pd_idx = p.pd_idx
        JOIN `user` u ON d.seller_idx2 = u.u_idx
        JOIN `check` c ON dl.ck_idx = c.ck_idx
        WHERE d.d_deal = 'DELIVERY'
        ORDER BY c.ck_idx DESC
        """, nativeQuery = true)
    List<Object[]> findAllCheckRows();
}
