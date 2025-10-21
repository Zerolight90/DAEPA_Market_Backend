package com.daepamarket.daepa_market_backend.domain.Category;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface CtLowRepository extends JpaRepository<CtLowEntity, Long> {
    List<CtLowEntity> findByMiddle_MiddleIdx(Long middleIdx);

    Optional<CtLowEntity> findByLowCt(String lowCt);
}
