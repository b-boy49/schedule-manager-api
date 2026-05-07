import { Module } from '@nestjs/common';
import { ConfigModule } from '@nestjs/config';
import { AuthModule } from './auth/auth.module';
import { DmModule } from './dm/dm.module';
import { UsersModule } from './users/users.module';
import { PrismaService } from './prisma.service';
import { RedisService } from './redis/redis.service';

@Module({
  imports: [ConfigModule.forRoot({ isGlobal: true }), AuthModule, UsersModule, DmModule],
  providers: [PrismaService, RedisService]
})
export class AppModule {}
