# Stage 1: Build application with dependency caching
FROM eclipse-temurin:25-jdk-noble AS builder
WORKDIR /build

# Copy Maven wrapper and POM to leverage layer cache for dependencies
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN chmod +x mvnw && ./mvnw dependency:go-offline -B

# Copy application source code
COPY src/ ./src/

# Build Spring Boot executable JAR and normalize artifact name
RUN ./mvnw clean package -DskipTests -B && \
    cp target/ams-*.jar /build/app.jar

# Stage 2: Minimal runtime image
FROM eclipse-temurin:25-jre-noble AS runner
WORKDIR /app

# Install curl for container health check
RUN apt-get update && \
    apt-get install -y --no-install-recommends curl && \
    rm -rf /var/lib/apt/lists/*

ARG UID=1000
ARG GID=1000

# Setup non-root application user matching host UID/GID for bind mount compatibility
RUN if getent passwd ubuntu >/dev/null 2>&1; then userdel -r ubuntu 2>/dev/null || true; fi && \
    if getent group "${GID}" >/dev/null 2>&1; then groupdel $(getent group "${GID}" | cut -d: -f1) 2>/dev/null || true; fi && \
    groupadd -g "${GID}" ams && \
    useradd -u "${UID}" -g ams -m -d /app ams

# Create persistent storage directories and assign ownership
RUN mkdir -p /app/data/uploads && chown -R ams:ams /app

USER ams

# Copy packaged application JAR and demo seed assets
COPY --from=builder --chown=ams:ams /build/app.jar /app/app.jar
COPY --chown=ams:ams demo/ /app/demo/

ENV JDK_JAVA_OPTIONS="-XX:+UseG1GC -XX:MaxRAMPercentage=75.0" \
    SERVER_PORT=8080

EXPOSE 8080

VOLUME ["/app/data"]

HEALTHCHECK --interval=30s --timeout=5s --start-period=30s --retries=3 \
    CMD ["curl", "-f", "http://localhost:8080/login"]

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
