package com.daepamarket.daepa_market_backend.common;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class FileStorageService {

    @Value("${app.upload.dir:uploads}")
    private String uploadDir;

    @Value("${app.upload.base-url:http://localhost:8080/uploads}")
    private String baseUrl;

    public List<String> saveAll(List<MultipartFile> files) {
        List<String> urls = new ArrayList<>();
        if (files == null) return urls;

        File dir = new File(uploadDir);
        if (!dir.exists()) dir.mkdirs();

        for (MultipartFile f : files) {
            if (f.isEmpty()) continue;
            String ext = getExt(f.getOriginalFilename());
            String name = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS"))
                    + "_" + Math.abs(f.getOriginalFilename().hashCode()) + (ext.isEmpty() ? "" : "." + ext);
            File dest = new File(dir, name);
            try {
                f.transferTo(dest);
                urls.add(baseUrl.endsWith("/") ? (baseUrl + name) : (baseUrl + "/" + name));
            } catch (IOException e) {
                // 개별 파일 실패는 건너뜀(필요 시 로깅/예외 처리 강화)
            }
        }
        return urls;
    }

    private String getExt(String filename) {
        if (filename == null) return "";
        int i = filename.lastIndexOf('.');
        return (i > -1) ? filename.substring(i + 1) : "";
    }
}
