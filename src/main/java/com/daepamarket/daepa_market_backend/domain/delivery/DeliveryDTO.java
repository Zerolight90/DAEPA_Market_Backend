package com.daepamarket.daepa_market_backend.domain.delivery;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class DeliveryDTO {

    private Long dealId;        // d_idx

    private Long deliveryId;    // dv_idx
    private Integer deliveryStatus; // dv_status

    private Integer checkStatus;    // ck_status
    private Integer checkResult;    // ck_result

    // loc_key 필요하면 여기 추가
    private Long locKey;

    private Long agreedPrice;

    private  String productTitle;

    private LocalDateTime dvDate;
}
