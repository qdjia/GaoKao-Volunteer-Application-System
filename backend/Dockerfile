FROM maven:3.9.11-eclipse-temurin-17-alpine AS build
WORKDIR /workspace

COPY pom.xml ./
COPY src ./src
RUN --mount=type=cache,target=/root/.m2 mvn --batch-mode --no-transfer-progress -DskipTests package \
    && cp target/gaokao-zhiyuan-1.0.0.jar /workspace/application.jar

FROM eclipse-temurin:17-jre-alpine
RUN addgroup -S gaokao && adduser -S -G gaokao gaokao
WORKDIR /app

COPY --from=build --chown=gaokao:gaokao /workspace/application.jar /app/application.jar

USER gaokao
EXPOSE 8080
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75.0", "-Djava.security.egd=file:/dev/urandom", "-jar", "/app/application.jar"]
