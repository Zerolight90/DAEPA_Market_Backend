package com.daepamarket.daepa_market_backend.domain.delivery;

import org.apache.ibatis.annotations.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface DeliveryRepository extends JpaRepository<DeliveryEntity, Long> {
    // 보낸 택배 거래의 배송/검수 상태 조회
    @Query("""
        select new com.daepamarket.daepa_market_backend.domain.delivery.DeliveryDTO(
            d.dIdx,
            dv.dvIdx,
            dv.dvStatus,
            ck.ckStatus,
            ck.ckResult,
            loc.locKey,
            d.agreedPrice,
            d.product.pdTitle
        )
        from DeliveryEntity dv
        join dv.deal d
        left join dv.location loc
        left join dv.checkEntity ck
        where d.seller.uIdx = :uIdx
        """)
    List<DeliveryDTO> findSentParcelsBySeller(@Param("uIdx") Long uIdx);

    // [받은 택배]
    @Query("""
        select new com.daepamarket.daepa_market_backend.domain.delivery.DeliveryDTO(
            d.dIdx,
            dv.dvIdx,
            dv.dvStatus,
            ck.ckStatus,
            ck.ckResult,
            loc.locKey,
            d.agreedPrice,
            d.product.pdTitle
        )
        from DeliveryEntity dv
        join dv.deal d
        left join dv.location loc
        left join dv.checkEntity ck
        where d.buyer.uIdx = :uIdx
        """)
    List<DeliveryDTO> findReceivedParcelsByBuyer(@Param("uIdx") Long uIdx);

    // 판매 내역에서 배송 보냄 확인 버튼
    @Query("select d from DeliveryEntity d where d.deal.dIdx = :dealId")
    Optional<DeliveryEntity> findByDealId(@Param("dealId") Long dealId);
}



