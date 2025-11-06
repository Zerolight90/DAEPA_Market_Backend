package com.daepamarket.daepa_market_backend.domain.delivery;

import com.daepamarket.daepa_market_backend.domain.deal.DealEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface deliveryRepository extends JpaRepository<DealEntity, Long> {
    // 배송 (보낸 택배, 받은 택배)
//    List<deliveryEntity> findByDealAndDvStatus(DealEntity deal, Integer dvStatus);
}


