package com.daepamarket.daepa_market_backend.admin.user;

import com.daepamarket.daepa_market_backend.domain.user.UserEntity;
import lombok.Getter;

@Getter
public class UserResponseDTO {

    private Long uIdx;
    private String uId;
    private String uName;
    private String uGender;
    private String uPhone;
    private String uDate;
    private String uBirth;
    private Double uManner;
    private Integer uWarn;
    private Integer uStatus;
    private String uLocation;

    public static UserResponseDTO of(UserEntity user) {
        UserResponseDTO dto = new UserResponseDTO();
        dto.uIdx = user.getUIdx();
        dto.uId = user.getUid();
        dto.uName = user.getUname();
        dto.uGender = user.getUGender();
        dto.uPhone = user.getUphone();
        dto.uDate = user.getUDate() != null ? user.getUDate().toString() : null;
        dto.uBirth = user.getUBirth() != null ? user.getUBirth().toString() : null;
        dto.uManner = user.getUManner();
        dto.uWarn = user.getUWarn();
        dto.uStatus = user.getUStatus();
        dto.uLocation = user.getULocation();
        return dto;


    }
}
