package com.daepamarket.daepa_market_backend.domain.Category;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface CtUpperRepository extends JpaRepository<CtUpperEntity, Long> {

    Optional<CtUpperEntity> findByUpperCt(String upperCt);

}
