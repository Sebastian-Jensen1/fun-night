# One image that serves both the web app and the API on port 8080.
#   docker build -t funnight .
#   docker run -p 8080:8080 -v funnight-data:/data funnight

# 1. Build the web app
FROM node:22-alpine AS web
WORKDIR /web
COPY web/package.json web/package-lock.json ./
RUN npm ci
COPY web/ ./
RUN npm run build

# 2. Build the server with the web app bundled in as static files
FROM eclipse-temurin:17-jdk AS server
WORKDIR /server
COPY server/ ./
COPY --from=web /web/dist ./src/main/resources/static
RUN ./mvnw -B -q -DskipTests package

# 3. Run it as an unprivileged user
FROM eclipse-temurin:17-jre
RUN useradd --system --create-home funnight && mkdir -p /data && chown funnight /data
USER funnight
WORKDIR /app
COPY --from=server /server/target/funnight-server.jar app.jar
# Saved game nights live on the /data volume. A hosted server sits behind a proxy, so trust its
# forwarded client address for rate limiting, and don't advertise internal network addresses.
ENV FUNNIGHT_DATA_DIR=/data/rooms \
    FUNNIGHT_FORWARD_HEADERS=native \
    FUNNIGHT_LAN_DISCOVERY=false
VOLUME /data
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
