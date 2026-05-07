import { io } from 'socket.io-client';

export function createSocket(token: string) {
  return io(process.env.NEXT_PUBLIC_WS_URL || 'http://localhost:4000', {
    transports: ['websocket'],
    auth: { token }
  });
}
