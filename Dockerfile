# Используем официальный образ Gradle с JDK 21
FROM gradle:8.10-jdk21 AS build_image

# Устанавливаем рабочую директорию
WORKDIR /app

# Аргумент сборки с токеном для чтения пакетов в github
ARG GH_TOKEN

# Копируем файлы конфигурации для кэширования зависимостей
COPY build.gradle settings.gradle ./
COPY tg-chats-collector-api/build.gradle ./tg-chats-collector-api/

# Прокидываем токен в gradle.properties
RUN mkdir -p ~/.gradle && \
    echo "githubPackagesReadToken=${GH_TOKEN}" >> ~/.gradle/gradle.properties

# Загружаем зависимости отдельным слоем (для кеширования)
RUN gradle dependencies --no-daemon

# Копируем исходный код
COPY src ./src
COPY tg-chats-collector-api/src ./tg-chats-collector-api/src

# Собираем приложение
RUN gradle build -x test --no-daemon


# Runtime образ
FROM eclipse-temurin:21-jre

# Создаем директории для нативных библиотек
RUN mkdir -p /app/

# Устанавливаем необходимые зависимости
RUN apt-get update && apt-get install -y \
    openssl \
    zlib1g \
    libc++1 \
    && rm -rf /var/lib/apt/lists/*

COPY --from=build_image /app/build/tg-chats-collector-dev-runner.jar /app/app.jar
COPY --from=build_image /app/build/native-libs/tdjni* /app/lib/
COPY --from=build_image /app/build/native-libs/libtdjni* /app/lib/

EXPOSE 8083
ENV TZ=Europe/Moscow

# Запускаем приложение
ENTRYPOINT ["java", "-Djava.library.path=/app/lib/", "-jar", "/app/app.jar"]
