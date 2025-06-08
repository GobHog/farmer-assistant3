FROM eclipse-temurin:11-jdk-alpine

WORKDIR /app

# предполагается, что jar-файл уже собран через Gradle
COPY build/libs/farmer-assistant3-all.jar app.jar


EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]

