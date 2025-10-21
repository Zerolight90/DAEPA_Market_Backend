package com.daepamarket.daepa_market_backend.domain.Category;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface CtMiddleRepository extends JpaRepository<CtMiddleEntity, Long> {
    List<CtMiddleEntity> findByUpper_UpperIdx(Long upperIdx);

    Optional<CtMiddleEntity> findByMiddleCt(String middleCt);
}
