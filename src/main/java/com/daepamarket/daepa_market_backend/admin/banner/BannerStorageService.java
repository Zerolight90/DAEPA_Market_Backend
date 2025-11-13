package com.daepamarket.daepa_market_backend.admin.banner;

import com.daepamarket.daepa_market_backend.S3Service;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.annotation.PostConstruct;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Service
public class BannerStorageService {

    private static final String S3_BANNERS_JSON_KEY = "banners/banners.json";

    private final S3Service s3Service;
    private final ObjectMapper objectMapper;

    public BannerStorageService(S3Service s3Service) {
        this.s3Service = s3Service;
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
        this.objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    @PostConstruct
    public void initialize() {
        try {
            // S3에 배너 JSON 파일이 없으면 빈 배열로 초기화
            if (!s3Service.fileExists(S3_BANNERS_JSON_KEY)) {
                saveBanners(new ArrayList<>());
            }
        } catch (Exception ex) {
            throw new IllegalStateException("배너 저장소를 초기화하는 중 오류가 발생했습니다.", ex);
        }
    }

    public synchronized List<BannerRecord> loadBanners() {
        try {
            String jsonContent = s3Service.downloadJsonFile(S3_BANNERS_JSON_KEY);
            if (jsonContent == null || jsonContent.trim().isEmpty()) {
                return new ArrayList<>();
            }
            return objectMapper.readValue(jsonContent, new TypeReference<List<BannerRecord>>() {});
        } catch (IOException ex) {
            throw new IllegalStateException("배너 데이터를 불러오는 중 오류가 발생했습니다.", ex);
        }
    }

    public synchronized void saveBanners(List<BannerRecord> banners) {
        try {
            String jsonContent = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(banners);
            s3Service.uploadJsonFile(jsonContent, S3_BANNERS_JSON_KEY);
        } catch (IOException ex) {
            throw new IllegalStateException("배너 데이터를 저장하는 중 오류가 발생했습니다.", ex);
        }
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BannerRecord {
        private Long id;
        private String imageUrl;
        private Integer displayOrder;
        private Boolean active;
        private String title;
        private String subtitle;
        private java.time.LocalDateTime createdAt;
        private java.time.LocalDateTime updatedAt;
    }
}
