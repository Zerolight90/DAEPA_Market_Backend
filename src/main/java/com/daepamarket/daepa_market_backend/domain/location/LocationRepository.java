package com.daepamarket.daepa_market_backend.domain.location;

import com.daepamarket.daepa_market_backend.domain.user.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface LocationRepository extends JpaRepository<LocationEntity, Long> {
    List<LocationEntity> findByUser(UserEntity user);

    Optional<LocationEntity> findById(Long locKey);

    long countByUser(UserEntity user);

    @Query("SELECT l FROM LocationEntity l WHERE l.user.uIdx = :uIdx")
    List<LocationEntity> findByUserId(@Param("uIdx") Long uIdx);
}
