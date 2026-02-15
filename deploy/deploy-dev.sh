#!/bin/bash

###############################################################################
# Скрипт выполняет сборку и локальный деплой для тестирования                 #
# Использует application-prod.properties из директории deploy                 #
###############################################################################


PROJECT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
LOCAL_DIR="$PROJECT_DIR/deploy/local"
CONFIG_FILE="$(dirname "$0")/application-prod.properties"


set -e

### Создание локальной директории
echo "Creating local directory..."
mkdir -p "$LOCAL_DIR"


### Копирование application-prod.properties в локальную директорию
echo "Copying application-prod.properties to local directory..."
cp "$CONFIG_FILE" "$LOCAL_DIR/"


### Сборка
echo "Building project..."
cd "$PROJECT_DIR"
./gradlew clean quarkusBuild

# Убираем номер коммита и dirty суффикс, оставляя только название сервиса и версию в имени jar файла
JAR_FILE=$(ls build/tg-chats-collector-*-runner.jar)
VERSION=$(echo "$JAR_FILE" | sed -E 's/.*tg-chats-collector-([0-9]+\.[0-9]+\.[0-9]+-RC[0-9]+).*/\1/')
NEW_JAR_NAME="tg-chats-collector-${VERSION}-runner.jar"

echo "Removing old jar files from local directory..."
rm -f "$LOCAL_DIR"/*.jar

echo "Copying jar to local directory..."
cp "$JAR_FILE" "$LOCAL_DIR/$NEW_JAR_NAME"


### Запуск для создания сессии
cd "$LOCAL_DIR"

echo "Run simple command to create tdlight session..."
java -jar "$NEW_JAR_NAME" last-chats

echo ""
echo "Local deployment completed successfully!"
echo "Directory: $LOCAL_DIR"
echo ""
echo "To run commands manually:"
echo "  cd $LOCAL_DIR"
echo "  java -jar $NEW_JAR_NAME search-history --chat-id=<ID> --from=<DATE>"