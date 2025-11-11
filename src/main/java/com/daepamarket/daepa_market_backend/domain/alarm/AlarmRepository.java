package com.daepamarket.daepa_market_backend.domain.alarm;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface AlarmRepository extends JpaRepository<AlarmEntity, Long> {

    @Transactional
    @Modifying
    @Query("DELETE FROM AlarmEntity a WHERE a.user.uIdx = :uIdx AND a.product.pdIdx = :productId")
    void deleteByUIdxAndProductId(@Param("uIdx") Long uIdx, @Param("productId") Long productId);

    @Query("SELECT a FROM AlarmEntity a JOIN a.product p " +
           "WHERE a.user.uIdx = :uIdx " +
           "AND p.ctLow.middle.upper.upperCt = :upperCategory " +
           "AND p.ctLow.middle.middleCt = :middleCategory " +
           "AND p.ctLow.lowCt = :lowCategory " +
           "AND p.pdPrice BETWEEN :minPrice AND :maxPrice " +
           "AND a.alDel = false")
    List<AlarmEntity> findMatchingAlarmsForUser(
            @Param("uIdx") Long uIdx,
            @Param("upperCategory") String upperCategory,
            @Param("middleCategory") String middleCategory,
            @Param("lowCategory") String lowCategory,
            @Param("minPrice") Integer minPrice,
            @Param("maxPrice") Integer maxPrice
    );
}
