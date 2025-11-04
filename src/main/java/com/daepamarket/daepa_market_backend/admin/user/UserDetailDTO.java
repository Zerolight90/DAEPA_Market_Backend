package com.daepamarket.daepa_market_backend.admin.user;

import com.daepamarket.daepa_market_backend.domain.user.UserEntity;
import lombok.Getter;
import lombok.Setter;
import java.util.List;

@Getter
@Setter
public class UserDetailDTO {

    private Long uIdx;
    private String uId;
    private String uName;
    private String uNickname;
    private String uGender;
    private String uLocation;
    private String uPhone;
    private Double uManner;
    private Integer uWarn;
    private Integer uStatus;
    private String uDate;
    private String uBirth;

    // 거래 내역
    private List<TradeHistoryDTO> tradeHistory;
    // 신고 내역
    private List<ReportHistoryDTO> reportHistory;

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
