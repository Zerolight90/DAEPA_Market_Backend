package com.daepamarket.daepa_market_backend.domain.Category;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import java.util.List;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@Entity
@Table(name = "ct_upper")
public class CtUpperEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "upper_idx")
    private Long upperIdx;

    @Column(name = "upper_ct", length = 30)
    private String upperCt;

    @OneToMany(mappedBy = "upper", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonIgnore
    private List<CtMiddleEntity> middles;
}