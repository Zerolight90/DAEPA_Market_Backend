package com.daepamarket.daepa_market_backend.domain.stop;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface StopRepository extends JpaRepository<StopEntity, Long> {
    @Query("SELECT COUNT(s) > 0 FROM StopEntity s WHERE s.user.uIdx = :uIdx")
    boolean existsByUserUIdx(@Param("uIdx") Long uIdx);
}
