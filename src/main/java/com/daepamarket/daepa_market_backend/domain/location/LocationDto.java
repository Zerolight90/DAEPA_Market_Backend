package com.daepamarket.daepa_market_backend.domain.location;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LocationDto {
    private Long locKey;
    private String locAddress;
    private String locDetail;
    private boolean locDefault;
    private String locCode;
    private String locTitle;
    private String locName;
    private String locNum;

    public LocationDto(LocationEntity entity) {
        this.locKey = entity.getLocKey();
        this.locAddress = entity.getLocAddress();
        this.locDetail = entity.getLocDetail();
        this.locDefault = entity.isLocDefault();
        this.locCode = entity.getLocCode();
        this.locTitle = entity.getLocTitle();
        this.locName = entity.getLocName();
        this.locNum = entity.getLocNum();
    }
}
