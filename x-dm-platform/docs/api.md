# API Design

## Auth
- `POST /auth/register`
- `POST /auth/login`
- `POST /auth/refresh`
- `POST /auth/logout`
- `GET /auth/me`

## Users
- `GET /users/search?q=`
- `GET /users/:id`

## DM
- `GET /dm/conversations`
- `POST /dm/conversations` (participantUserId)
- `GET /dm/conversations/:id/messages?cursor=`
- `POST /dm/conversations/:id/messages` (body)
- `PATCH /dm/messages/:id/read`

## Response contract
- success: `{ data, meta?, error: null }`
- error: `{ data: null, error: { code, message, details? } }`
