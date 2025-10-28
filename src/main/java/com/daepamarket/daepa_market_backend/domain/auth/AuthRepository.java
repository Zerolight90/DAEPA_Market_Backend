package com.daepamarket.daepa_market_backend.domain.auth;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AuthRepository extends JpaRepository<AuthEntity, Long> {
    Optional<AuthEntity> findTopByAuthEmailOrderByAuthIdxDesc(String authEmail);


}
