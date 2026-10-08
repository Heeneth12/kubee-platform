# Build from the repository root:
#   docker build -f services/inventory/Dockerfile -t kubee-inventory .

# 1. Build stage: compile this service plus the shared libraries it depends on
FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /workspace

COPY pom.xml .
COPY libs libs
COPY services services

RUN mvn -q -B -pl services/inventory -am package -DskipTests

# 2. Run stage
FROM eclipse-temurin:21-jre
WORKDIR /app

COPY --from=build /workspace/services/inventory/target/*.jar app.jar

# The port comes from the PORT env var (defaults to 8085)
EXPOSE 8085

ENTRYPOINT ["java","-jar","app.jar"]
