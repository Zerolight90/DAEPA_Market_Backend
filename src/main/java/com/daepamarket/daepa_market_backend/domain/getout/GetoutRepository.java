package com.daepamarket.daepa_market_backend.domain.getout;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface GetoutRepository extends JpaRepository<GetoutEntity, Long> {
    @Query("SELECT COUNT(g) > 0 FROM GetoutEntity g WHERE g.user.uIdx = :uIdx")
    boolean existsByUserUIdx(@Param("uIdx") Long uIdx);
}
