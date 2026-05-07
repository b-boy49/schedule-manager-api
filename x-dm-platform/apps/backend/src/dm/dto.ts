import { IsString, MinLength } from 'class-validator';

export class CreateConversationDto {
  @IsString() participantUserId!: string;
}

export class SendMessageDto {
  @IsString() @MinLength(1) body!: string;
}
