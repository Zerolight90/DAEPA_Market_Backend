package com.daepamarket.daepa_market_backend.domain.deal;

import jakarta.persistence.LockModeType;
import com.daepamarket.daepa_market_backend.domain.user.UserEntity;
import org.apache.ibatis.annotations.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DealRepository extends JpaRepository<DealEntity, Long> {

    Optional<DealEntity> findByProduct_PdIdx(Long pdIdx);

    @Query("SELECT d FROM DealEntity d JOIN FETCH d.product WHERE d.buyer.uIdx = :uIdx")
    List<DealEntity> findByBuyer_uIdx(Long uIdx);

    @Query("SELECT d FROM DealEntity d JOIN FETCH d.product WHERE d.seller.uIdx = :uIdx")
    List<DealEntity> findBySeller_uIdx(Long uIdx);

    // 상품 기준으로 Deal을 찾되, 비관적 쓰기 락(PESSIMISTIC_WRITE)을 거는 메소드
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<DealEntity> findWithWriteLockByProduct_PdIdx(Long pdIdx);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT d FROM DealEntity d WHERE d.dIdx = :dIdx")
    Optional<DealEntity> findWithWriteLockByDIdx(@Param("dIdx") Long dIdx);

    // 안전결제 내역
    @Query("""

            select new com.daepamarket.daepa_market_backend.domain.deal.DealSafeDTO(
            p.pdTitle,
            d.agreedPrice,
            d.dEdate
            )
            from DealEntity d
            join d.product p
            where d.seller.uIdx = :uIdx
            order by d.dEdate desc
        """)
    List<DealSafeDTO> findSettlementsBySeller(@Param("uIdx") Long uIdx);

    // 판매 내역
    @Query("""
    select new com.daepamarket.daepa_market_backend.domain.deal.DealSellHistoryDTO(
        d.dIdx,
        p.pdIdx,
        p.pdTitle,
        d.dEdate,
        d.agreedPrice,
        d.dSell,
        d.dBuy,
        d.dStatus,
        TRIM(d.dDeal),
        dv.dvStatus,
        ck.ckStatus
    )
    from com.daepamarket.daepa_market_backend.domain.deal.DealEntity d
    join d.product p
    left join com.daepamarket.daepa_market_backend.domain.delivery.DeliveryEntity dv
        on dv.deal = d
    left join com.daepamarket.daepa_market_backend.domain.check.CheckEntity ck
        on ck = dv.checkEntity
    where d.seller.uIdx = :sellerIdx
    order by d.dEdate desc
    """)
    List<DealSellHistoryDTO> findSellHistoryBySeller(@Param("sellerIdx") Long sellerIdx);

}