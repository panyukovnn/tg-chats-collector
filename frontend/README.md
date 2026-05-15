# tg-chats-collector frontend

Минималистичный React-клиент в стиле Telegram для сервиса tg-chats-collector.

## Возможности

- Слева — список последних чатов с предпросмотром (автор + текст последнего сообщения).
- При выборе чата справа доступна кнопка «Скачать».
- В диалоге задаётся период `dateFrom`/`dateTo`; по нажатию «Скачать» фронт сам формирует JSON-файл с историей и сохраняет его через `Blob`.

## Разработка

```bash
npm install
npm run dev
```

Vite поднимется на `http://localhost:5173` и проксирует запросы `/tg-chats-collector/*` на бэкенд `http://localhost:8083`.

## Сборка

```bash
npm run build
```

## Docker

`Dockerfile` собирает статику и раздаёт через nginx. `nginx.conf` ожидает upstream `backend:8083` (имя сервиса бэкенда в общей docker-compose-сети).

```bash
docker build -t tg-chats-collector-frontend .
```
