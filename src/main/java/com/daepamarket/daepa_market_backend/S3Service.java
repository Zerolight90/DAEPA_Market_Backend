package com.daepamarket.daepa_market_backend;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;
import java.io.InputStream;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.UUID;
import java.util.Objects;

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
        // 파일명에 UUID를 추가하여 중복 방지
        String originalFilename = Objects.requireNonNull(file.getOriginalFilename());
        String extension = "";
        int lastDotIndex = originalFilename.lastIndexOf('.');
        if (lastDotIndex > 0) {
            extension = originalFilename.substring(lastDotIndex);
            originalFilename = originalFilename.substring(0, lastDotIndex);
        }
        String safeFilename = UUID.randomUUID().toString() + "_" + originalFilename.replaceAll("\\s+", "_") + extension;
        String key = folderName + "/" + safeFilename;

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

    /**
     * S3에 JSON 문자열을 파일로 업로드합니다.
     * @param jsonContent JSON 문자열
     * @param key S3 객체 키 (예: "banners/banners.json")
     * @return 업로드된 파일의 S3 URL
     */
    public String uploadJsonFile(String jsonContent, String key) {
        try {
            s3Client.putObject(
                    PutObjectRequest.builder()
                            .bucket(bucketName)
                            .key(key)
                            .contentType("application/json")
                            .build(),
                    RequestBody.fromString(jsonContent, StandardCharsets.UTF_8)
            );
            return String.format("https://%s.s3.ap-northeast-2.amazonaws.com/%s", bucketName, key);
        } catch (Exception e) {
            throw new RuntimeException("S3에 JSON 파일을 업로드하는 중 오류가 발생했습니다: " + e.getMessage(), e);
        }
    }

    /**
     * S3에서 JSON 파일을 읽어서 문자열로 반환합니다.
     * @param key S3 객체 키 (예: "banners/banners.json")
     * @return JSON 문자열 (파일이 없으면 null)
     */
    public String downloadJsonFile(String key) {
        try (InputStream inputStream = s3Client.getObject(
                GetObjectRequest.builder()
                        .bucket(bucketName)
                        .key(key)
                        .build()
        )) {
            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (NoSuchKeyException e) {
            // 파일이 없으면 null 반환
            return null;
        } catch (Exception e) {
            throw new RuntimeException("S3에서 JSON 파일을 다운로드하는 중 오류가 발생했습니다: " + e.getMessage(), e);
        }
    }

    /**
     * S3에 파일이 존재하는지 확인합니다.
     * @param key S3 객체 키
     * @return 파일이 존재하면 true, 아니면 false
     */
    public boolean fileExists(String key) {
        try {
            s3Client.headObject(
                    software.amazon.awssdk.services.s3.model.HeadObjectRequest.builder()
                            .bucket(bucketName)
                            .key(key)
                            .build()
            );
            return true;
        } catch (NoSuchKeyException e) {
            return false;
        } catch (Exception e) {
            // 다른 오류는 로깅만 하고 false 반환
            System.err.println("S3 파일 존재 확인 중 오류 발생: " + e.getMessage());
            return false;
        }
    }
}
