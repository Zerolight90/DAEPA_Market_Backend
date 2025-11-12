package com.daepamarket.daepa_market_backend.admin.user;

import com.daepamarket.daepa_market_backend.domain.user.UserEntity;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;
import java.util.List;

@Getter
@Setter
public class UserDetailDTO {

    @JsonProperty("uidx")
    private Long uIdx;
    
    @JsonProperty("uid")
    private String uId;
    
    @JsonProperty("uname")
    private String uName;
    
    @JsonProperty("unickname")
    private String uNickname;
    
    @JsonProperty("ugender")
    private String uGender;
    
    @JsonProperty("ulocation")
    private String uLocation;
    
    @JsonProperty("uphone")
    private String uPhone;
    
    @JsonProperty("umanner")
    private Double uManner;
    
    @JsonProperty("uwarn")
    private Integer uWarn;
    
    @JsonProperty("ustatus")
    private Integer uStatus;
    
    @JsonProperty("udate")
    private String uDate;
    
    @JsonProperty("ubirth")
    private String uBirth;

    // 거래 내역
    private List<TradeHistoryDTO> tradeHistory;
    // 신고 내역
    private List<ReportHistoryDTO> reportHistory;
    // 신고 횟수
    private Integer reportCount;
    // 리뷰 목록
    private List<com.daepamarket.daepa_market_backend.admin.review.AllReviewDTO> reviews;

    public static UserDetailDTO fromEntity(UserEntity e) {
        UserDetailDTO dto = new UserDetailDTO();
        dto.setUIdx(e.getUIdx());
        dto.setUId(e.getUid());
        dto.setUName(e.getUname());
        dto.setUGender(e.getUGender());
        dto.setUNickname(e.getUnickname());
        dto.setUPhone(e.getUphone());
        dto.setUManner(e.getUManner());
        dto.setUWarn(e.getUWarn());
        dto.setUStatus(e.getUStatus());
        dto.setUDate(e.getUDate() != null ? e.getUDate().toString() : null);
        dto.setUBirth(
                e.getUBirth() != null && e.getUBirth().length() == 8
                        ? e.getUBirth().substring(0,4) + "-" + e.getUBirth().substring(4,6) + "-" + e.getUBirth().substring(6)
                        : e.getUBirth()
        );

        return dto;
    }
}
