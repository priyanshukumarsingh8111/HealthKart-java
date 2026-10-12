# Stage 1: Runtime
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Install curl and tzdata for health checks and timezone support
RUN apk add --no-cache tzdata curl

# Set timezone
ENV TZ=Asia/Kolkata

# Create directories for persistent database and uploads
RUN mkdir -p /app/data /app/public

# Copy pre-built application jar
COPY target/healthkart-server-1.0.0.jar /app/app.jar

# Copy initial seed data and public assets
COPY data /app/data
COPY public /app/public

# Expose Spring Boot Web Port (3000) and Netty-SocketIO Port (9092)
EXPOSE 3000 9092

# Define volume for persistent database storage
VOLUME ["/app/data"]

# Environment defaults
ENV PORT=3000
ENV JAVA_OPTS="-Xms256m -Xmx512m -XX:+UseG1GC"

# Healthcheck to verify the web server is responding
HEALTHCHECK --interval=30s --timeout=5s --start-period=15s --retries=3 \
  CMD curl -f http://localhost:3000/servlets/health || exit 1

# Start the application
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar"]

