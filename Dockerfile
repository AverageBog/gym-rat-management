# syntax=docker/dockerfile:1

FROM node:20-alpine AS frontend-build
WORKDIR /frontend
COPY frontend/package.json frontend/package-lock.json* ./
RUN npm install --no-audit --no-fund
COPY frontend/ ./
RUN npm run build

FROM eclipse-temurin:17-jdk-jammy AS backend-build
WORKDIR /backend
COPY backend/ ./
COPY --from=frontend-build /frontend/dist ./src/main/resources/static
RUN chmod +x gradlew && ./gradlew --no-daemon clean bootJar

FROM eclipse-temurin:17-jre-jammy AS runtime
RUN useradd --system --uid 10001 --no-create-home --shell /usr/sbin/nologin spring
WORKDIR /app
COPY --from=backend-build /backend/build/libs/*.jar app.jar
ENV SPRING_PROFILES_ACTIVE=prod
EXPOSE 8080
USER spring
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
