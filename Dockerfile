FROM eclipse-temurin:11-jdk-alpine

WORKDIR /app

# Скопировать JAR
COPY build/libs/farmer-assistant3-all.jar app.jar

# Скопировать модель
COPY src/main/resources/ru_bert.onnx src/main/resources/ru_bert.onnx

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
