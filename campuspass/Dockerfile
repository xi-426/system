FROM maven:3.9.11-eclipse-temurin-17 AS builder
WORKDIR /workspace

COPY pom.xml .
COPY src ./src
RUN --mount=type=cache,target=/root/.m2 \
    mvn -B package -DskipTests

FROM eclipse-temurin:17-jre
WORKDIR /app

COPY --from=builder \
    /workspace/target/campuspass-0.0.1-SNAPSHOT.jar \
    app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
