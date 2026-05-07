import { IsEmail, IsString, MinLength } from 'class-validator';

export class RegisterDto {
  @IsString() username!: string;
  @IsEmail() email!: string;
  @IsString() @MinLength(8) password!: string;
}

export class LoginDto {
  @IsString() email!: string;
  @IsString() password!: string;
}
