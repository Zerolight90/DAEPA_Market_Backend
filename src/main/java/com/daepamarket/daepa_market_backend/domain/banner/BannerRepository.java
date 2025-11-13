package com.daepamarket.daepa_market_backend.domain.banner;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BannerRepository extends JpaRepository<BannerEntity, Long> {

    @Query("SELECT b FROM BannerEntity b ORDER BY b.bannerDisplayOrder ASC, b.bannerIdx DESC")
    List<BannerEntity> findAllOrderByDisplayOrder();

    @Query("SELECT b FROM BannerEntity b WHERE b.bannerActive = true ORDER BY b.bannerDisplayOrder ASC, b.bannerIdx DESC")
    List<BannerEntity> findActiveBannersOrderByDisplayOrder();
}


