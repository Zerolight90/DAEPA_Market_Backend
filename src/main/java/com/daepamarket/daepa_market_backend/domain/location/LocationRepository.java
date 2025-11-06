package com.daepamarket.daepa_market_backend.domain.location;

import com.daepamarket.daepa_market_backend.domain.user.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface LocationRepository extends JpaRepository<LocationEntity, Long> {
    List<LocationEntity> findByUser(UserEntity user);

    Optional<LocationEntity> findById(Long locKey);

    long countByUser(UserEntity user);
}
