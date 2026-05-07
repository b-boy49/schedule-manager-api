# WebSocket Events (Socket.IO)

## Client -> Server
- `conversation:join` `{ conversationId }`
- `conversation:leave` `{ conversationId }`
- `message:send` `{ conversationId, body, clientMessageId }`
- `message:read` `{ conversationId, messageId }`
- `typing:start` `{ conversationId }`
- `typing:stop` `{ conversationId }`

## Server -> Client
- `message:new` `{ conversationId, message }`
- `message:sent` `{ clientMessageId, messageId, sentAt }`
- `message:read` `{ conversationId, messageId, readAt, readerUserId }`
- `typing:update` `{ conversationId, userId, isTyping }`
- `presence:update` `{ userId, online }`

## Auth
- handshake auth: `{ token: <access-token> }`
