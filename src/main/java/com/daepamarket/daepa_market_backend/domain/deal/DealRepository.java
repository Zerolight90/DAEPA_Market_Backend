package com.daepamarket.daepa_market_backend.domain.deal;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface DealRepository extends JpaRepository<DealEntity, Long> {

    Optional<DealEntity> findByProduct_PdIdx(Long pdIdx);

    @Query("SELECT d FROM DealEntity d JOIN FETCH d.product WHERE d.buyer.uIdx = :uIdx")
    List<DealEntity> findByBuyer_uIdx(Long uIdx);

    @Query("SELECT d FROM DealEntity d JOIN FETCH d.product WHERE d.seller.uIdx = :uIdx")
    List<DealEntity> findBySeller_uIdx(Long uIdx);


}