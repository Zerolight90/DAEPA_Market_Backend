package com.daepamarket.daepa_market_backend.domain.product;

import com.daepamarket.daepa_market_backend.domain.Category.CtLowEntity;
import com.daepamarket.daepa_market_backend.domain.productimage.ProductImageEntity;
import com.daepamarket.daepa_market_backend.domain.user.UserEntity;
import jakarta.persistence.*;
import lombok.*;
import java.time.*;
import java.util.ArrayList;
import java.util.List;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@Entity
@Table(name = "product")
public class ProductEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "pd_idx")
    private Long pdIdx;

    // products(N:1) users(판매자)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "u_idx", nullable = false)
    private UserEntity seller;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ct_low", nullable = false)
    private CtLowEntity ctLow;

    @Column(name = "pd_price")
    private Long pdPrice;

    @Column(name = "pd_title", length = 100)
    private String pdTitle;

    @Column(name = "pd_content", length = 100)
    private String pdContent;

    @Column(name = "pd_location", length = 20)
    private String pdLocation;

    @Column(name = "pd_status")
    private Integer pdStatus;

    @Column(name = "pd_thumb", length = 250)
    private String pdThumb;

    @Column(name = "pd_hit")
    private Integer pdHit;

    @Column(name = "pd_ref")
    private Integer pdRef;

    @Column(name = "pd_create")
    private LocalDateTime pdCreate;

    @Column(name = "pd_update")
    private LocalDateTime pdUpdate;

    @Column(name = "pd_refdate")
    private LocalDateTime pdRefdate;

    @Column(name = "pd_edate")
    private LocalDate pdEdate;

    @Column(name = "pd_ip", length = 50)
    private String pdIp;

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ProductImageEntity> images = new ArrayList<>();
}
