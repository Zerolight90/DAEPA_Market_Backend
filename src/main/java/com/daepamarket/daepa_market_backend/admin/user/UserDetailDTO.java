package com.daepamarket.daepa_market_backend.admin.user;

import com.daepamarket.daepa_market_backend.domain.user.UserEntity;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;
import java.util.List;
import java.time.LocalDateTime;

@Getter
@Setter
public class UserDetailDTO {
    private Long uidx;
    private String uid;
    private String uname;
    private String unickname;
    private String uphone;
    private Integer ustatus;
    private Integer uwarn;
    private Double umanner;
    private LocalDateTime udate;
    private String ulocation;
    private List<TradeHistoryDTO> tradeHistory;
    private List<ReportHistoryDTO> reportHistory;
    private Integer reportCount;
    private List<com.daepamarket.daepa_market_backend.admin.review.AllReviewDTO> reviews;
    private String ubirth;
    private String ugender;

    public static UserDetailDTO fromEntity(UserEntity entity) {
        UserDetailDTO dto = new UserDetailDTO();
        dto.setUidx(entity.getUIdx());
        dto.setUid(entity.getUid());
        dto.setUname(entity.getUname());
        dto.setUnickname(entity.getUnickname());
        dto.setUphone(entity.getUphone());
        dto.setUstatus(entity.getUStatus());
        dto.setUwarn(entity.getUWarn());
        dto.setUmanner(entity.getUManner());
        dto.setUdate(entity.getUDate());
        dto.setUbirth(entity.getUBirth());
        dto.setUgender(entity.getUGender());
        return dto;
    }
}
