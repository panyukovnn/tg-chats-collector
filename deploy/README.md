
# Сборка и работа с проектом

## Локальный запуск команд

Собираем проект, деплоим локально
```shell
cd ..
./gradlew quarkusBuild -x test
cd ./deploy
./deploy-dev.sh
```

Запуск команды локально:
```shell
cd ./local
java -jar tg-chats-collector-3.0.0*-runner.jar \
     search-history --chat-id -437083490 --limit 50
```