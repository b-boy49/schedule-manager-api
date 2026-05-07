import { Controller, Get, Param, Query, UseGuards } from '@nestjs/common';
import { PrismaService } from '../prisma.service';
import { JwtAuthGuard } from '../common/guards/jwt-auth.guard';

@UseGuards(JwtAuthGuard)
@Controller('users')
export class UsersController {
  constructor(private readonly prisma: PrismaService) {}

  @Get('search')
  search(@Query('q') q = '') {
    return this.prisma.user.findMany({
      where: { username: { contains: q, mode: 'insensitive' } },
      select: { id: true, username: true, avatarUrl: true }
    });
  }

  @Get(':id')
  byId(@Param('id') id: string) {
    return this.prisma.user.findUnique({ where: { id }, select: { id: true, username: true, avatarUrl: true } });
  }
}
