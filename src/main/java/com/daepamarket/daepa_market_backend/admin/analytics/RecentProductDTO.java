package com.daepamarket.daepa_market_backend.admin.analytics;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RecentProductDTO {
    private Long id;
    private String name;
    private String seller;
    private Long price;
    private String category;
    private LocalDateTime createdAt;
}

