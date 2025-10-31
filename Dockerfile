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