# ---- 1) qurma ----
FROM eclipse-temurin:17-jdk-alpine AS build
WORKDIR /app

# Əvvəlcə yalnız asılılıq faylları - mənbə dəyişəndə Gradle keşi yenidən
# yüklənməsin deyə ayrı qatda saxlanılır.
COPY gradle gradle
COPY gradlew build.gradle settings.gradle ./
RUN chmod +x ./gradlew && ./gradlew --no-daemon dependencies || true

COPY src src
RUN ./gradlew --no-daemon bootJar -x test

# ---- 2) işlətmə ----
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
RUN addgroup -S signum && adduser -S signum -G signum
COPY --from=build /app/build/libs/gateway-service-0.0.1-SNAPSHOT.jar app.jar
USER signum
EXPOSE 8080
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "/app/app.jar"]
