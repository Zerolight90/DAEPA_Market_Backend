package com.daepamarket.daepa_market_backend.common;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
public class FileStorageService {
    @Value("${app.upload.dir:uploads}") // 프로젝트 루트 기준 기본값
    private String uploadDir;

    public List<String> saveAll(List<MultipartFile> files) {
        if (files == null || files.isEmpty()) return List.of();
        try {
            Path root = Paths.get(uploadDir).toAbsolutePath().normalize();
            Files.createDirectories(root);

            List<String> urls = new ArrayList<>();
            for (MultipartFile f : files) {
                if (f == null || f.isEmpty()) continue;
                String safeName = UUID.randomUUID() + "_" + Objects.requireNonNull(f.getOriginalFilename()).replaceAll("\\s+", "_");
                Path target = root.resolve(safeName);
                Files.copy(f.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
                // 클라이언트가 접근할 URL (정적 매핑 /uploads/**로 노출)
                urls.add("/uploads/" + safeName);
            }
            return urls;
        } catch (IOException e) {
            throw new RuntimeException("파일 저장 실패", e);
        }
    }
}

