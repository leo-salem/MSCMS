# Stage 1: Build
FROM maven:3.9.6-eclipse-temurin-21-jammy AS build
WORKDIR /app

COPY pom.xml .
RUN mvn dependency:go-offline -B

COPY src ./src
RUN mvn clean package -DskipTests -B

RUN find target -maxdepth 1 -name '*.jar' -not -name '*original*' | head -1 | xargs -I{} cp {} target/app.jar

# Stage 2: Runtime
FROM eclipse-temurin:21-jre-jammy
WORKDIR /app

RUN apt-get update && apt-get install -y --no-install-recommends curl && rm -rf /var/lib/apt/lists/*

COPY --from=build /app/target/app.jar app.jar

ENTRYPOINT ["java", "-jar", "app.jar"]
