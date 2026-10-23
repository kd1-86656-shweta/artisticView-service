# Builds the jar inside Docker, so Railway (or any host) can build straight from the repo.
FROM eclipse-temurin:21-jdk AS build
WORKDIR /src
COPY gradlew settings.gradle build.gradle dependencies.gradle ./
COPY gradle gradle
RUN chmod +x gradlew && ./gradlew --version --no-daemon >/dev/null
COPY . .
RUN ./gradlew :server:bootJar --no-daemon -q

FROM eclipse-temurin:21-jre
RUN groupadd --system app && useradd --system --gid app app && mkdir -p /data/media && chown app:app /data/media
WORKDIR /app
COPY --from=build /src/server/build/libs/app.jar app.jar
COPY --chmod=755 docker-entrypoint.sh /app/docker-entrypoint.sh
USER app
EXPOSE 8080
ENV SPRING_PROFILES_ACTIVE=prod \
    APP_MEDIA_LOCAL_DIR=/data/media \
    JAVA_OPTS="-XX:MaxRAMPercentage=70"
ENTRYPOINT ["/app/docker-entrypoint.sh"]
