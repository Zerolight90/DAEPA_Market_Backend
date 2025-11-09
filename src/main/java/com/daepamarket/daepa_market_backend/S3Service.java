package com.daepamarket.daepa_market_backend;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;

@Service
public class S3Service {
    private final S3Client s3Client;
    private final String bucketName = "daepa-s3";
    private final String bucketUrl = "https://daepa-s3.s3.ap-northeast-2.amazonaws.com/";

    public S3Service(
            @Value("${AWS_ACCESS_KEY_ID}") String accessKey,
            @Value("${AWS_SECRET_ACCESS_KEY}") String secretKey
    ) {
        AwsBasicCredentials awsCreds = AwsBasicCredentials.create(accessKey, secretKey);

        this.s3Client = S3Client.builder()
                .region(Region.AP_NORTHEAST_2) // 서울 리전
                .credentialsProvider(StaticCredentialsProvider.create(awsCreds))
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

    public void deleteFile(String fileUrl) {
        // 파일 URL이 비어있거나 null인 경우 아무 작업도 하지 않음
        if (!StringUtils.hasText(fileUrl)) {
            return;
        }

        try {
            // 전체 URL에서 S3 객체 키(파일 경로)를 추출
            String key = fileUrl.substring(bucketUrl.length());
            // URL 인코딩된 키를 디코딩 (예: 한글 파일명 등)
            String decodedKey = URLDecoder.decode(key, StandardCharsets.UTF_8);

            s3Client.deleteObject(DeleteObjectRequest.builder()
                    .bucket(bucketName)
                    .key(decodedKey)
                    .build());
        } catch (Exception e) {
            // S3에서 파일을 찾지 못하는 등 예외가 발생할 수 있으나,
            // 어차피 삭제하려는 것이므로 오류를 로깅만 하고 넘어감.
            System.err.println("S3 파일 삭제 중 오류 발생: " + e.getMessage());
        }
    }

    private String saveTempFile(MultipartFile file) throws IOException {
        java.nio.file.Path tempPath = java.nio.file.Files.createTempFile("upload-", file.getOriginalFilename());
        file.transferTo(tempPath.toFile());
        return tempPath.toString();
    }
}
