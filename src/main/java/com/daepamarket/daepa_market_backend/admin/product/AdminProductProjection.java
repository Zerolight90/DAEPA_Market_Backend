package com.daepamarket.daepa_market_backend.admin.product;

import java.time.LocalDateTime;

public interface AdminProductProjection {
    Long getPdIdx();
    String getPdTitle();
    Long getPdPrice();
    String getPdThumb();
    LocalDateTime getPdCreate();
    Long getSellerId();
    String getSellerName();
    String getUpperCt();
    String getMiddleCt();
    String getLowCt();
    Integer getReportCount();
    Long getDealStatus();
    Long getDealSell();
}


