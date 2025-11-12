package com.daepamarket.daepa_market_backend.admin.banner;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.annotation.PostConstruct;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

@Service
public class BannerStorageService {

    private final ObjectMapper objectMapper;
    private final Path storagePath;

    public BannerStorageService(@Value("${app.banner.storage-file:storage/banners.json}") String storageFile) {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
        this.objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        this.storagePath = Paths.get(storageFile).toAbsolutePath();
    }

    @PostConstruct
    public void initialize() {
        try {
            Path parent = storagePath.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            if (Files.notExists(storagePath)) {
                saveBanners(new ArrayList<>());
            }
        } catch (IOException ex) {
            throw new IllegalStateException("배너 저장소를 초기화하는 중 오류가 발생했습니다.", ex);
        }
    }

    public synchronized List<BannerRecord> loadBanners() {
        try {
            if (Files.notExists(storagePath)) {
                return new ArrayList<>();
            }
            return objectMapper.readValue(storagePath.toFile(), new TypeReference<List<BannerRecord>>() {});
        } catch (IOException ex) {
            throw new IllegalStateException("배너 데이터를 불러오는 중 오류가 발생했습니다.", ex);
        }
    }

    public synchronized void saveBanners(List<BannerRecord> banners) {
        try {
            // 부모 디렉토리가 없으면 생성
            Path parent = storagePath.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            // 파일이 없거나 삭제되었을 경우를 대비하여 항상 저장
            // 파일이 없으면 자동으로 생성됨
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(storagePath.toFile(), banners);
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
