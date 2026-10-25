# syntax=docker/dockerfile:1
# Builds the allocator from source: docker build -t call-allocation-service .

# The Gradle toolchain asks for Java 21 and does not download it, so the build stage must provide it.
FROM eclipse-temurin:21-jdk AS build
WORKDIR /src
COPY . .
# The cache mount keeps Gradle's distribution and dependency downloads between builds.
RUN --mount=type=cache,target=/root/.gradle \
    ./gradlew --no-daemon :app:installDist

FROM eclipse-temurin:21-jre
# Same non-root user the k8s deployment runs as.
USER 65532:65532
WORKDIR /app
COPY --from=build /src/app/build/install/app /app
EXPOSE 8080
ENTRYPOINT ["/app/bin/app"]
