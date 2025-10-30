package com.daepamarket.daepa_market_backend;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;
import java.nio.file.Paths;

@Service
public class S3Service {

    private final S3Client s3Client;
    private final String bucketName = "daepa-s3";

    public S3Service() {
        this.s3Client = S3Client.builder()
                .region(Region.AP_NORTHEAST_2) // 서울 리전
                .credentialsProvider(DefaultCredentialsProvider.create())
                .build();
    }

    public String uploadFile(MultipartFile file, String folderName) throws IOException {
        String key = folderName + "/" + file.getOriginalFilename();

        s3Client.putObject(
                PutObjectRequest.builder()
                        .bucket(bucketName)
                        .key(key)
                        .contentType(file.getContentType())
                        .build(),
                Paths.get(saveTempFile(file)) // 파일을 임시 경로로 저장 후 업로드
        );

        return String.format("https://%s.s3.ap-northeast-2.amazonaws.com/%s", bucketName, key);
    }

    private String saveTempFile(MultipartFile file) throws IOException {
        java.nio.file.Path tempPath = java.nio.file.Files.createTempFile("upload-", file.getOriginalFilename());
        file.transferTo(tempPath.toFile());
        return tempPath.toString();
    }
}