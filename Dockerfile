# Используем официальный образ Gradle с JDK 17
FROM gradle:8.10-jdk17 AS build_image

# Устанавливаем рабочую директорию
WORKDIR /app

# Аргумент сборки с токеном для чтения пакетов в github
ARG GH_TOKEN

# Копируем файлы конфигурации для кеширования зависимостей
COPY build.gradle settings.gradle ./

# Прокидываем токен в gradle.properties
RUN mkdir -p ~/.gradle && \
    echo "githubPackagesReadToken=${GH_TOKEN}" >> ~/.gradle/gradle.properties

# Копируем исходный код
COPY src ./src

# Собираем приложение
RUN gradle build -x test --no-daemon


# Runtime образ
FROM eclipse-temurin:17-jre

# Создаем директории для нативных библиотек
RUN mkdir -p /app/lib
RUN mkdir -p /app/

# Устанавливаем необходимые зависимости
RUN apt-get update && apt-get install -y \
    openssl \
    zlib1g \
    libc++1 \
    && rm -rf /var/lib/apt/lists/*

COPY --from=build_image /app/build/libs/tg-chats-collector*.jar /app/
COPY --from=build_image /app/build/libs/tdjni* /app/lib/
COPY --from=build_image /app/build/libs/libtdjni* /app/lib/

EXPOSE 8083
ENV TZ=Europe/Moscow

# Добавляем путь к библиотекам в LD_LIBRARY_PATH
ENV LD_LIBRARY_PATH=/app/lib:${LD_LIBRARY_PATH}

# Запускаем приложение
ENTRYPOINT ["java", "-Djava.library.path=/app/lib/", "-jar", "/app/tg-chats-collector.jar"]
