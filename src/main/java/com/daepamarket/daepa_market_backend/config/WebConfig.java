package com.daepamarket.daepa_market_backend.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.resource.PathResourceResolver;

import java.nio.file.Path;
import java.nio.file.Paths;

@Configuration
public class WebConfig implements WebMvcConfigurer {


    /** 브라우저가 접근하는 URL 프리픽스 (기본: /uploads) */
    @Value("${app.upload.url-prefix:/uploads}")
    private String urlPrefix;

    /** 실제 파일이 저장된 디렉터리 (기본: ./uploads) */
    @Value("${app.upload.dir:uploads}")
    private String uploadDir;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 실제 경로를 절대경로로 정규화
        Path uploadPath = Paths.get(uploadDir).toAbsolutePath().normalize();

        // file: 스킴 + 끝에 슬래시 보장
        String location = "file:" + uploadPath.toString() + (uploadPath.toString().endsWith("/") ? "" : "/");

        // URL 패턴도 슬래시 정리
        String pattern = (urlPrefix.endsWith("/")) ? (urlPrefix + "**") : (urlPrefix + "/**");

        registry.addResourceHandler(pattern)
                .addResourceLocations(location)
                .setCachePeriod(3600)              // 필요 시 캐시(초). 개발 중엔 0으로
                .resourceChain(true)
                .addResolver(new PathResourceResolver());
    }
}
