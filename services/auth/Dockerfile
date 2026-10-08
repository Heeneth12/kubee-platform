# Build from the repository root:
#   docker build -f services/auth/Dockerfile -t kubee-auth .

# 1. Build stage: compile this service plus the shared libraries it depends on
FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /workspace

COPY pom.xml .
COPY libs libs
COPY services services

RUN mvn -q -B -pl services/auth -am package -DskipTests

# 2. Run stage
FROM eclipse-temurin:21-jre
WORKDIR /app

COPY --from=build /workspace/services/auth/target/*.jar app.jar

# The port comes from the PORT env var (defaults to 8080)
EXPOSE 8080

ENTRYPOINT ["java","-jar","app.jar"]
