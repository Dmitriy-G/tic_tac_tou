FROM maven:3.9.9-eclipse-temurin-21-alpine AS builder
WORKDIR /build
COPY pom.xml ./
COPY services/ services/
RUN mvn clean package -pl services/tictactoe-engine -am \
    --batch-mode --no-transfer-progress -Dmaven.test.skip=true

FROM eclipse-temurin:21-jre-alpine
RUN addgroup -g 1001 -S appgroup && adduser -u 1001 -S -G appgroup appuser
WORKDIR /app
COPY --from=builder --chown=appuser:appgroup /build/services/tictactoe-engine/target/*.jar app.jar
USER appuser
ENTRYPOINT ["java","-jar","app.jar"]