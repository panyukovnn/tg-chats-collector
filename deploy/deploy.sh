#!/bin/bash

#################################################################################
# Скрипт выполняет полную подготовку и деплой .jar файла на сервер              #
# Предварительно необходимо подготовить только файл application-prod.properties #
#################################################################################


SSH_CONFIG=tabots
LOCAL_DIR=/Users/nikolay/dev/shell-services/tg-chats-collector
REMOTE_DIR=shell-services/tg-chats-collector
CONFIG_FILE=./application-prod.properties


set -e

### Создание директорий
echo "Creating local directory..."
mkdir -p "$LOCAL_DIR" 2> /dev/null

echo "Creating remote directory..."
ssh "$SSH_CONFIG" "mkdir -p ~/$REMOTE_DIR"


### Копирование application-prod.properties в локальную директорию сборки и на сервер

echo "Copying application-prod.properties to local directory..."
cp "$CONFIG_FILE" "$LOCAL_DIR"
echo "Copying application-prod.properties to remote server..."
scp "$CONFIG_FILE" "$SSH_CONFIG:~/$REMOTE_DIR/"


### Сборка

cd ..

echo "Building project..."
./gradlew clean quarkusBuild

# Убираем номер коммита и dirty суффикс, оставляя только название сервиса и версию в имени jar файла
JAR_FILE=$(ls build/tg-chats-collector-*-runner.jar)
VERSION=$(echo "$JAR_FILE" | sed -E 's/.*tg-chats-collector-([0-9]+\.[0-9]+\.[0-9]+-RC[0-9]+).*/\1/')
NEW_JAR_NAME="tg-chats-collector-${VERSION}-runner.jar"

echo "Removing old jar files from local directory..."
rm -f "$LOCAL_DIR"/*.jar

echo "Copying jar to local directory..."
cp "$JAR_FILE" "$LOCAL_DIR/$NEW_JAR_NAME"

echo "Removing old jar files from remote directory..."
ssh "$SSH_CONFIG" "mkdir -p ~/$REMOTE_DIR && rm -f ~/$REMOTE_DIR/*.jar"

echo "Copying jar to remote server..."
scp "$JAR_FILE" "$SSH_CONFIG:~/$REMOTE_DIR/$NEW_JAR_NAME"


### Создание сессии и отправка бд с сессией на удаленный сервер

cd "$LOCAL_DIR"

echo "Run simple command to create tdlight session"
java -jar tg-chats-collector-*-runner.jar last-chats

echo "Copying tdlight-session directory to remote server..."
scp -r "./tdlight-session" "$SSH_CONFIG:~/$REMOTE_DIR/"


echo "Deployment completed successfully!"
