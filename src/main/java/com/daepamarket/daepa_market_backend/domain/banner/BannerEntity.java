package com.daepamarket.daepa_market_backend.domain.banner;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "banner")
public class BannerEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "banner_idx")
    private Long bannerIdx;

    @Column(name = "banner_title", length = 200)
    private String bannerTitle;

    @Column(name = "banner_subtitle", length = 500)
    private String bannerSubtitle;

    @Column(name = "banner_image_url", length = 500)
    private String bannerImageUrl;

    @Column(name = "banner_display_order")
    private Integer bannerDisplayOrder;

    @Column(name = "banner_active")
    private Boolean bannerActive;

    @Column(name = "banner_created_at")
    private LocalDateTime bannerCreatedAt;

    @Column(name = "banner_updated_at")
    private LocalDateTime bannerUpdatedAt;

    @PrePersist
    protected void onCreate() {
        bannerCreatedAt = LocalDateTime.now();
        bannerUpdatedAt = LocalDateTime.now();
        if (bannerActive == null) {
            bannerActive = true;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        bannerUpdatedAt = LocalDateTime.now();
    }
}


