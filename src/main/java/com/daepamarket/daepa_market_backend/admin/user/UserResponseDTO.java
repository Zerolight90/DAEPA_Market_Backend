package com.daepamarket.daepa_market_backend.admin.user;

import com.daepamarket.daepa_market_backend.domain.user.UserEntity;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

@Getter
public class UserResponseDTO {

    @JsonProperty("uidx")
    private Long uIdx;
    
    @JsonProperty("uid")
    private String uId;
    
    @JsonProperty("uname")
    private String uName;
    
    @JsonProperty("ugender")
    private String uGender;
    
    @JsonProperty("uphone")
    private String uPhone;
    
    @JsonProperty("udate")
    private String uDate;
    
    @JsonProperty("ubirth")
    private String uBirth;
    
    @JsonProperty("umanner")
    private Double uManner;
    
    @JsonProperty("uwarn")
    private Integer uWarn;
    
    @JsonProperty("ustatus")
    private Integer uStatus;
    
    @JsonProperty("ulocation")
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
        dto.uLocation = null; // 주소는 별도로 설정
        return dto;
    }

    public static UserResponseDTO of(UserEntity user, String location) {
        UserResponseDTO dto = of(user);
        dto.uLocation = location;
        return dto;
    }
}
