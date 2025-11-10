package com.daepamarket.daepa_market_backend.oneonone;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;

@Getter
@Builder
public class UserOneOnOneResponseDTO {
    private Long Idx;
    private Integer Status;
    private String Title;
    private String Content;
    private String Photo;
    private LocalDate Date;
    private String Re;

    // 👈 여기! 엔티티에는 없지만 응답으로는 보냄
    private String writer;
}
