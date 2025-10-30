package com.daepamarket.daepa_market_backend.admin.oneonone.DTO;

import lombok.*;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OneOnOneResponseDTO {

    private Long id;
    private String name;
    private String title;
    private String content;
    private String photo;
    private Integer category;
    private LocalDate date;
}
