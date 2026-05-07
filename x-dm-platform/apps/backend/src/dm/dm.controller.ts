import { Body, Controller, Get, Param, Patch, Post, Req, UseGuards } from '@nestjs/common';
import { JwtAuthGuard } from '../common/guards/jwt-auth.guard';
import { DmService } from './dm.service';
import { CreateConversationDto, SendMessageDto } from './dto';

@UseGuards(JwtAuthGuard)
@Controller('dm')
export class DmController {
  constructor(private readonly dmService: DmService) {}

  @Get('conversations')
  conversations(@Req() req: { user: { sub: string } }) {
    return this.dmService.listConversations(req.user.sub);
  }

  @Post('conversations')
  createConversation(@Req() req: { user: { sub: string } }, @Body() dto: CreateConversationDto) {
    return this.dmService.createConversation(req.user.sub, dto);
  }

  @Get('conversations/:id/messages')
  messages(@Param('id') id: string) {
    return this.dmService.listMessages(id);
  }

  @Post('conversations/:id/messages')
  send(@Req() req: { user: { sub: string } }, @Param('id') id: string, @Body() dto: SendMessageDto) {
    return this.dmService.sendMessage(req.user.sub, id, dto);
  }

  @Patch('messages/:id/read')
  read(@Param('id') id: string) {
    return this.dmService.markAsRead(id);
  }
}
