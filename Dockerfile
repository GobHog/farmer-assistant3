FROM ubuntu:22.04

# Обновление и установка необходимых зависимостей
RUN apt-get update && apt-get install -y \
    openjdk-11-jdk \
    ca-certificates \
    libstdc++6 \
    libgomp1 \
    libatomic1 \
    && rm -rf /var/lib/apt/lists/*

# Создание рабочей директории
WORKDIR /app

# Копируем JAR и модель
COPY build/libs/farmer-assistant3-all.jar app.jar
COPY src/main/resources/ru_bert.onnx src/main/resources/ru_bert.onnx

# Открываем порт
EXPOSE 8080

# Команда запуска
ENTRYPOINT ["java", "-jar", "app.jar"]
