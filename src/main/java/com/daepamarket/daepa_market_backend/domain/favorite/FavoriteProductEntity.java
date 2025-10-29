package com.daepamarket.daepa_market_backend.domain.favorite;

import com.daepamarket.daepa_market_backend.domain.product.ProductEntity;
import com.daepamarket.daepa_market_backend.domain.user.UserEntity;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

// com.daepamarket.daepa_market_backend.domain.favorite.FavoriteProductEntity
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@Entity
@Table(
        name = "favorite_product", // ✅ 확실히 스네이크/소문자로 고정
        uniqueConstraints = @UniqueConstraint(
                name = "uk_favorite_user_product",
                columnNames = {"u_idx", "pd_idx"} // ✅ 'pd_idx'로 통일
        ),
        indexes = {
                @Index(name = "idx_fav_user", columnList = "u_idx"),
                @Index(name = "idx_fav_pd", columnList = "pd_idx")
        }
)
public class FavoriteProductEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "f_idx")
    private Long fIdx;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "u_idx", nullable = false)
    private UserEntity user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pd_idx", nullable = false) // ✅ 외래키 컬럼명 통일
    private ProductEntity product;

    @Column(name = "f_status", nullable = false)
    private Boolean status; // true=찜, false=해제

    @Column(name = "f_date", nullable = false)
    private LocalDateTime fDate;
}
