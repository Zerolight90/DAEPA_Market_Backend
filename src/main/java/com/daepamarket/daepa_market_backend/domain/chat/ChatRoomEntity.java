package com.daepamarket.daepa_market_backend.domain.chat;

import com.daepamarket.daepa_market_backend.domain.deal.DealEntity;
import com.daepamarket.daepa_market_backend.domain.product.ProductEntity;
import jakarta.persistence.*;
import lombok.*;
import java.time.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@Entity
@Table(name = "chat_rooms")
public class ChatRoomEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ch_idx")
    private Long chIdx;

    // chat_rooms(N:1) products
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pd_idx", nullable = false)
    private ProductEntity product;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "d_idx", nullable = true)
    private DealEntity deal;

    @Column(name = "ch_identifier")
    private String chIdentifier;

    @Column(name = "ch_created")
    private LocalDateTime chCreated;

    @Column(name = "ch_updated")
    private LocalDateTime chUpdated;
}
