package com.daepamarket.daepa_market_backend.domain.Category;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import java.util.List;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@Entity
@Table(name = "ct_middle")
public class CtMiddleEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "middle_idx")
    private Long middleIdx;

    @Column(name = "middle_ct", length = 30)
    private String middleCt;

    // 상위 카테고리 연결 (N:1)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "upper_idx")
    @JsonIgnore
    private CtUpperEntity upper;

    // 하위 카테고리 연결 (1:N)
    @OneToMany(mappedBy = "middle", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonIgnore
    private List<CtLowEntity> lowers;
}
