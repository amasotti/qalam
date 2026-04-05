# =============================================================================
# Qalam Backend
# =============================================================================

# ---------- Build stage ----------
FROM gradle:9.4.1-jdk25-corretto-al2023 AS builder

WORKDIR /app

COPY gradle gradle
COPY gradlew settings.gradle.kts build.gradle.kts ./
RUN chmod +x gradlew

# cache dependencies
RUN ./gradlew dependencies --no-daemon || true

COPY src src

# build distribution instead of fat jar
RUN ./gradlew installDist --no-daemon


# ---------- Runtime stage ----------
FROM amazoncorretto:25.0.2-alpine3.23

# install doppler + tini
RUN apk add --no-cache tini wget && \
    wget -q -t3 \
    'https://packages.doppler.com/public/cli/rsa.8004D9FF50437357.key' \
    -O /etc/apk/keys/cli@doppler-8004D9FF50437357.rsa.pub && \
    echo 'https://packages.doppler.com/public/cli/alpine/any-version/main' \
    >> /etc/apk/repositories && \
    apk add --no-cache doppler

# create non-root user (alpine style)
RUN addgroup -S ktor && adduser -S -u 1001 -G ktor ktor

WORKDIR /app

COPY --from=builder /app/build/install/ /app/

RUN chown -R ktor:ktor /app
USER ktor

EXPOSE 8085

ENV JAVA_OPTS="-XX:+UseContainerSupport \
               -XX:MaxRAMPercentage=75 \
               -XX:+ExitOnOutOfMemoryError"

STOPSIGNAL SIGTERM

ENTRYPOINT ["/sbin/tini", "--"]
CMD ["sh", "-c", "doppler run -- /app/qalam-backend/bin/qalam-backend"]
