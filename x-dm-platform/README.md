# X-like DM Platform (Production-ready Scaffold)

Next.js + NestJS + PostgreSQL + Prisma + Socket.IO + JWT + Redis + Docker Compose のDM基盤です。

## Folder structure

```txt
x-dm-platform/
  apps/
    backend/
      src/
      prisma/
      Dockerfile
    frontend/
      app/
      components/
      lib/
      Dockerfile
  docs/
    api.md
    websocket.md
  infra/nginx/
    nginx.conf
  docker-compose.yml
  .env.example
```

## Quick start

1. `cp .env.example .env`
2. `docker compose up --build`
3. Frontend: `http://localhost:3000`
4. Backend: `http://localhost:4000`

## Notes

- Auth: access token (short), refresh token (long, HttpOnly cookie)
- Redis: refresh token blacklist / websocket presence / pubsub
- Socket.IO: JWT handshake + room per conversation
