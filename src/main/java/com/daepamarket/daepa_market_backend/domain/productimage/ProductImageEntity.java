package com.daepamarket.daepa_market_backend.domain.productimage;

import com.daepamarket.daepa_market_backend.domain.product.ProductEntity;
import jakarta.persistence.*;
import lombok.*;
import java.time.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@Entity
@Table(name = "product_image")
public class ProductImageEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "pi_idx")
    private Long piIdx;

    // product_images(N:1) products
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pd_idx", nullable = false)
    private ProductEntity product;

    @Column(name = "image_url", length = 250)
    private String imageUrl;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "pd_lowerimg", length = 250)
    private String pdLowerimg;
}
