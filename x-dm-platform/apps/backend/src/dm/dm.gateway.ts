import {
  ConnectedSocket,
  MessageBody,
  OnGatewayConnection,
  SubscribeMessage,
  WebSocketGateway,
  WebSocketServer
} from '@nestjs/websockets';
import { Server, Socket } from 'socket.io';
import { JwtService } from '@nestjs/jwt';
import { DmService } from './dm.service';

@WebSocketGateway({ cors: { origin: process.env.CORS_ORIGIN, credentials: true } })
export class DmGateway implements OnGatewayConnection {
  @WebSocketServer() server!: Server;

  constructor(private readonly jwtService: JwtService, private readonly dmService: DmService) {}

  handleConnection(client: Socket) {
    const token = client.handshake.auth?.token as string | undefined;
    if (!token) return client.disconnect();
    try {
      const payload = this.jwtService.verify(token, { secret: process.env.JWT_ACCESS_SECRET });
      client.data.userId = payload.sub;
    } catch {
      client.disconnect();
    }
  }

  @SubscribeMessage('conversation:join')
  join(@ConnectedSocket() client: Socket, @MessageBody() body: { conversationId: string }) {
    client.join(`conversation:${body.conversationId}`);
  }

  @SubscribeMessage('message:send')
  async send(
    @ConnectedSocket() client: Socket,
    @MessageBody() body: { conversationId: string; body: string; clientMessageId?: string }
  ) {
    const message = await this.dmService.sendMessage(client.data.userId, body.conversationId, { body: body.body });
    this.server.to(`conversation:${body.conversationId}`).emit('message:new', { conversationId: body.conversationId, message });
    if (body.clientMessageId) {
      client.emit('message:sent', { clientMessageId: body.clientMessageId, messageId: message.id, sentAt: message.createdAt });
    }
  }

  @SubscribeMessage('typing:start')
  typingStart(@ConnectedSocket() client: Socket, @MessageBody() body: { conversationId: string }) {
    client.to(`conversation:${body.conversationId}`).emit('typing:update', {
      conversationId: body.conversationId,
      userId: client.data.userId,
      isTyping: true
    });
  }

  @SubscribeMessage('typing:stop')
  typingStop(@ConnectedSocket() client: Socket, @MessageBody() body: { conversationId: string }) {
    client.to(`conversation:${body.conversationId}`).emit('typing:update', {
      conversationId: body.conversationId,
      userId: client.data.userId,
      isTyping: false
    });
  }
}
