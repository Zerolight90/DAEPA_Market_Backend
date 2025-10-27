package com.daepamarket.daepa_market_backend.domain.favorite;

import com.daepamarket.daepa_market_backend.domain.user.UserEntity;
import com.daepamarket.daepa_market_backend.domain.product.ProductEntity;
import jakarta.persistence.*;
import lombok.*;
import java.time.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@Entity
@Table(
        name = "favoriteProduct",
        uniqueConstraints = @UniqueConstraint(name = "UK_favorite_user_product", columnNames = {"u_idx","p_idx"})
)
public class FavoriteProductEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "f_idx")
    private Long fIdx;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "u_idx", nullable = false)
    private UserEntity user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "p_idx", nullable = false)
    private ProductEntity product;

    @Column(name = "f_status")
    private Boolean status;

    @Column(name = "f_date")
    private LocalDateTime fDate;
}
