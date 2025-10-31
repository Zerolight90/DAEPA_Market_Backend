## --- 1. 빌드 스테이지 ---
## JDK 17과 Gradle이 설치된 이미지를 빌드 환경으로 사용
#FROM gradle:8.5.0-jdk17 AS build
#
## 작업 디렉토리 설정
#WORKDIR /app
#
## 소스코드 전체 복사 (별도 레포지토리이므로)
#COPY . .
#
## Gradle을 사용해 프로젝트 빌드 (CI/CD 파이프라인 속도를 위해 테스트는 제외)
#RUN ./gradlew build -x test
#
## --- 2. 실행 스테이지 ---
## 실제 실행을 위한 가벼운 JRE 이미지 사용
#FROM amazoncorretto:17-alpine-jre
#
#WORKDIR /app
#
## 빌드 스테이지에서 생성된 .jar 파일만 복사
#COPY --from=build /app/build/libs/*.jar app.jar
#
## 컨테이너 시작 시 실행될 명령어
#ENTRYPOINT ["java", "-jar", "app.jar"]

FROM gradle:8.5-jdk17-alpine AS builder

WORKDIR /app

# Gradle 캐싱을 위해 먼저 의존성 다운로드
COPY build.gradle settings.gradle ./
COPY gradle ./gradle
RUN gradle dependencies --no-daemon || true

# 소스 코드 복사 및 빌드
COPY . .
RUN gradle clean bootJar --no-daemon -x test

# Stage 2: Runtime
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

# 타임존 설정
RUN apk add --no-cache tzdata && \
    cp /usr/share/zoneinfo/Asia/Seoul /etc/localtime && \
    echo "Asia/Seoul" > /etc/timezone

# 애플리케이션 사용자 생성
RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

# 빌드된 JAR 파일 복사
COPY --from=builder /app/build/libs/*.jar app.jar

# 헬스체크
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8080/actuator/health || exit 1

# 실행
ENTRYPOINT ["java", \
  "-XX:+UseContainerSupport", \
  "-XX:MaxRAMPercentage=75.0", \
  "-Djava.security.egd=file:/dev/./urandom", \
  "-jar", \
  "app.jar"]

EXPOSE 8080