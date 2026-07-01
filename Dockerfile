# ---------- 빌드 스테이지: Gradle 로 실행 가능한 JAR 생성 ----------
FROM eclipse-temurin:17-jdk-alpine AS builder
WORKDIR /app

COPY gradlew settings.gradle build.gradle ./
COPY gradle ./gradle
COPY src ./src

RUN chmod +x gradlew && ./gradlew bootJar --no-daemon -x test \
	&& cp "$(ls build/libs/*.jar | grep -v plain)" /app/app.jar

# ---------- 실행 스테이지: JRE 만 포함해 이미지 크기 축소 ----------
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

COPY --from=builder /app/app.jar app.jar

EXPOSE 8080

ENV JAVA_OPTS=""
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
