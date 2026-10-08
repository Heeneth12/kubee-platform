# Build from the repository root:
#   docker build -f services/pos/Dockerfile -t kubee-pos .

# 1. Build stage: compile this service plus the shared libraries it depends on
FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /workspace

COPY pom.xml .
COPY libs libs
COPY services services

RUN mvn -q -B -pl services/pos -am package -DskipTests

# 2. Run stage
FROM eclipse-temurin:21-jre
WORKDIR /app

COPY --from=build /workspace/services/pos/target/*.jar app.jar

# The port comes from the PORT env var (defaults to 8086)
EXPOSE 8086

ENTRYPOINT ["java","-jar","app.jar"]
