package com.daepamarket.daepa_market_backend.domain.userpick;

import java.util.List;

import com.daepamarket.daepa_market_backend.domain.Category.CtLowEntity;
import com.daepamarket.daepa_market_backend.domain.Category.CtMiddleEntity;
import com.daepamarket.daepa_market_backend.domain.Category.CtUpperEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import com.daepamarket.daepa_market_backend.domain.user.UserEntity;


@Repository
public interface UserPickRepository extends JpaRepository<UserPickEntity, Long> {

    // @Query("SELECT ALL FROM UserPickEntity WHERE user = '김토스'")
    List<UserPickEntity> findByUser(UserEntity user);

    // 상품 정보와 매칭되는 UserPick 목록 조회 쿼리
    @Query("SELECT up FROM UserPickEntity up " +
            "WHERE up.ctLow = :low " + "AND up.minPrice <= :price AND up.maxPrice >= :price")
    List<UserPickEntity> findMatchingPicks(
            @Param("low") CtLowEntity low,
            @Param("price") Long price // ProductEntity의 가격 타입에 맞게 int 또는 Long
    );

}
