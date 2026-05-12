# Stage 1: Build
FROM maven:3.9.6-eclipse-temurin-21-jammy AS build
WORKDIR /app

COPY pom.xml .
RUN mvn dependency:go-offline -B

COPY src ./src
RUN mvn clean package -DskipTests -B && test -f target/app.jar

# Stage 2: Runtime
FROM eclipse-temurin:21-jre-jammy
WORKDIR /app

RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/* \
    && groupadd --system --gid 1000 spring \
    && useradd --system --uid 1000 --gid spring --home-dir /app spring

COPY --from=build --chown=spring:spring /app/target/app.jar app.jar

USER spring:spring

EXPOSE 8088

# Override via `docker run -e JAVA_OPTS=...` or compose `environment`
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75.0 -XX:+ExitOnOutOfMemoryError"

HEALTHCHECK --interval=30s --timeout=5s --start-period=120s --retries=5 \
    CMD curl -fsS http://127.0.0.1:8088/actuator/health >/dev/null || exit 1

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar app.jar"]
