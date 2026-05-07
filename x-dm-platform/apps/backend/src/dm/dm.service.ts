import { Injectable } from '@nestjs/common';
import { PrismaService } from '../prisma.service';
import { CreateConversationDto, SendMessageDto } from './dto';

@Injectable()
export class DmService {
  constructor(private readonly prisma: PrismaService) {}

  listConversations(userId: string) {
    return this.prisma.conversation.findMany({
      where: { participants: { some: { userId } } },
      include: { participants: { include: { user: { select: { id: true, username: true, avatarUrl: true } } } }, messages: { take: 1, orderBy: { createdAt: 'desc' } } },
      orderBy: { updatedAt: 'desc' }
    });
  }

  async createConversation(userId: string, dto: CreateConversationDto) {
    return this.prisma.conversation.create({
      data: {
        participants: {
          create: [{ userId }, { userId: dto.participantUserId }]
        }
      },
      include: { participants: true }
    });
  }

  listMessages(conversationId: string) {
    return this.prisma.message.findMany({ where: { conversationId }, orderBy: { createdAt: 'asc' }, take: 50 });
  }

  async sendMessage(senderId: string, conversationId: string, dto: SendMessageDto) {
    const msg = await this.prisma.message.create({
      data: { conversationId, senderId, body: dto.body }
    });
    await this.prisma.conversation.update({ where: { id: conversationId }, data: { updatedAt: new Date() } });
    return msg;
  }

  markAsRead(messageId: string) {
    return this.prisma.message.update({ where: { id: messageId }, data: { readAt: new Date() } });
  }
}
