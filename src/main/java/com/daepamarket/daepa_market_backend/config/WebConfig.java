// com.daepamarket.daepa_market_backend.config.WebConfig
package com.daepamarket.daepa_market_backend.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.web.servlet.config.annotation.CorsRegistry;   // ✅ 추가
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.resource.PathResourceResolver;

import java.nio.file.Path;
import java.nio.file.Paths;

@Configuration
@Profile("dev")
public class WebConfig implements WebMvcConfigurer {

    @Value("${app.upload.url-prefix:/uploads}")
    private String urlPrefix;

    @Value("${app.upload.dir:uploads}")
    private String uploadDir;

    /*
    // ✅ CORS 정확히 추가 (콜론 포함)
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOrigins(
                        "http://localhost:3000",
                        "http://3.34.181.73",  // ← 반드시 콜론 포함!
                        "https://daepamarket.shop"
                        // 필요 시, 배포 도메인도 여기에 추가
                )
                .allowedMethods("GET","POST","PUT","PATCH","DELETE","OPTIONS")
                .allowedHeaders("*")
                .exposedHeaders("Authorization", "Set-Cookie", "Content-Disposition")
                .allowCredentials(true)
                .maxAge(3600);
    }
    */

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        Path uploadPath = Paths.get(uploadDir).toAbsolutePath().normalize();
        String location = "file:" + uploadPath.toString() + (uploadPath.toString().endsWith("/") ? "" : "/");
        String pattern = (urlPrefix.endsWith("/")) ? (urlPrefix + "**") : (urlPrefix + "/**");

        registry.addResourceHandler(pattern)
                .addResourceLocations(location)
                .setCachePeriod(3600)
                .resourceChain(true)
                .addResolver(new PathResourceResolver());
    }
}
