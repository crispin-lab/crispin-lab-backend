# Multi-stage build for app module. Build context: repository root.
# Usage: docker build -f app/Dockerfile -t crispin-lab:dev .

FROM eclipse-temurin:21-jdk-jammy AS builder
WORKDIR /workspace

# Step 1: build descriptors only — dependency resolution caches independently of source.
COPY gradlew ./
COPY gradle gradle
COPY settings.gradle.kts build.gradle.kts ./
COPY build-logic build-logic
COPY app/build.gradle.kts app/build.gradle.kts
COPY lab-common/build.gradle.kts lab-common/build.gradle.kts
COPY lab-common-infra/build.gradle.kts lab-common-infra/build.gradle.kts
COPY lab-api-support/build.gradle.kts lab-api-support/build.gradle.kts
COPY lab-space/domain/build.gradle.kts lab-space/domain/build.gradle.kts
COPY lab-space/app/build.gradle.kts lab-space/app/build.gradle.kts

RUN --mount=type=cache,target=/root/.gradle \
    ./gradlew --no-daemon :app:dependencies -q

# Step 2: full source + bootJar.
COPY . .

RUN --mount=type=cache,target=/root/.gradle \
    ./gradlew --no-daemon :app:bootJar -x test

FROM eclipse-temurin:21-jre-jammy AS runtime
WORKDIR /app

COPY --from=builder /workspace/app/build/libs/app.jar /app/app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
