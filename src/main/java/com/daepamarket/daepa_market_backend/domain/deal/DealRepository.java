package com.daepamarket.daepa_market_backend.domain.deal;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface DealRepository extends JpaRepository<DealEntity, Long> {

    Optional<DealEntity> findByProduct_PdIdx(Long pdIdx);

}