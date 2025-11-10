package com.daepamarket.daepa_market_backend.domain.alarm;

import com.daepamarket.daepa_market_backend.domain.product.ProductEntity;
import com.daepamarket.daepa_market_backend.domain.user.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public interface AlarmRepository extends JpaRepository<AlarmEntity, Long> {

    @Transactional
    @Modifying
    void deleteByUserAndProduct(UserEntity user, ProductEntity product);

}
