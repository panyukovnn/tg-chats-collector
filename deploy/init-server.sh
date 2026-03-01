#!/bin/bash

# Конфигурация
SSH_CONFIG=tabots
REMOTE_DIR=tg-chats-collector

# Проверка на наличие файлов рядом со скриптом
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
COMPOSE_FILE="$SCRIPT_DIR/docker-compose.yml"
APPLICATION_PROD_FILE="$SCRIPT_DIR/application-prod.properties"
# TODO удалить синхронизацию tdlight-session после написания инструкции по созданию сессии на сервере самостоятельно
TDLIGHT_SESSION_DIR="$SCRIPT_DIR/../tdlight-session"

if [[ ! -f "$COMPOSE_FILE" || ! -f "$APPLICATION_PROD_FILE" ]]; then
  echo "Ошибка: не найден docker-compose.yml или application-prod.properties рядом со скриптом."
  exit 1
fi

if [[ ! -d "$TDLIGHT_SESSION_DIR" ]]; then
  echo "Ошибка: не найдена директория tdlight-session рядом со скриптом."
  exit 1
fi

echo "Синхронизация файлов на сервер..."
rsync -avz --progress \
  "$COMPOSE_FILE" \
  "$APPLICATION_PROD_FILE" \
  "$SSH_CONFIG:$REMOTE_DIR/"

echo "Синхронизация директории tdlight-session на сервер..."
rsync -avz --progress \
  "$TDLIGHT_SESSION_DIR/" \
  "$SSH_CONFIG:$REMOTE_DIR/tdlight-session/"

echo "Готово. Файлы успешно синхронизированы."