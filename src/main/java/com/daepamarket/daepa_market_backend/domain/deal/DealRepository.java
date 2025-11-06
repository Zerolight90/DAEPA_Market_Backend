package com.daepamarket.daepa_market_backend.domain.deal;

import jakarta.persistence.LockModeType;
import com.daepamarket.daepa_market_backend.domain.user.UserEntity;
import org.apache.ibatis.annotations.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DealRepository extends JpaRepository<DealEntity, Long> {

    Optional<DealEntity> findByProduct_PdIdx(Long pdIdx);

    @Query("SELECT d FROM DealEntity d JOIN FETCH d.product WHERE d.buyer.uIdx = :uIdx")
    List<DealEntity> findByBuyer_uIdx(Long uIdx);

    @Query("SELECT d FROM DealEntity d JOIN FETCH d.product WHERE d.seller.uIdx = :uIdx")
    List<DealEntity> findBySeller_uIdx(Long uIdx);

    // 상품 기준으로 Deal을 찾되, 비관적 쓰기 락(PESSIMISTIC_WRITE)을 거는 메소드
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<DealEntity> findWithWriteLockByProduct_PdIdx(Long pdIdx);
    // 안전결제 내역
    @Query("""
           select d
           from DealEntity d
           join fetch d.product p
           where d.seller = :seller
           """)
    List<DealEntity> findWithProductBySeller(@Param("seller") UserEntity seller);

    List<DealEntity> findBySeller(UserEntity seller);

}
