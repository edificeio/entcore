import { Inject, Injectable } from '@nestjs/common';
import { ClientProxy } from '@nestjs/microservices';
import { firstValueFrom } from 'rxjs';
import { RegisterI18nFilesRequestDTO, RegisterI18nFilesResponseDTO } from './{application}-i18n-translations-register.types';

@Injectable()
export class {application}I18nTranslationsRegisterClient {

  constructor(
    @Inject('NATS_CLIENT') private readonly natsClient: ClientProxy
  ) {
    console.log('Creating events service')
  }

  
    
  async request(event: RegisterI18nFilesRequestDTO): Promise<RegisterI18nFilesResponseDTO> {
    const eventAddress = this.getSubject('{application}.i18n.translations.register');
    const reply = await firstValueFrom(this.natsClient.send(eventAddress, event));
    return reply as RegisterI18nFilesResponseDTO;
  }
        


  private getSubject(subjectPattern: string, ...addressComplements: string[]): string {
    let subject: string;
    if(addressComplements) {
      let parts = subjectPattern.split(/[*>]/);
      for(let i = 0; i < addressComplements.length; i++) {
        parts.splice(2 * i + 1, 0, addressComplements[i]);
      }
      subject = parts.join('');
    } else {
      subject = subjectPattern;
    }
    return subject;
  }

}
