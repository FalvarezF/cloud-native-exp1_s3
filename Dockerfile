FROM --platform=$BUILDPLATFORM maven:3.9.9-eclipse-temurin-17 AS build

WORKDIR /workspace

COPY pom.xml .
COPY src ./src

RUN mvn -B -ntp verify

FROM eclipse-temurin:17-jre-jammy

WORKDIR /app

RUN groupadd --gid 10001 spring \
    && useradd --uid 10001 --gid spring --no-create-home --home-dir /app spring

COPY --from=build /workspace/target/sistema-tickets-0.0.1-SNAPSHOT.jar /app/app.jar

ENV EFS_DIRECTORY=/app/tickets
EXPOSE 8080

USER spring:spring

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
