import { Inject, Injectable } from '@nestjs/common';
import { ClientProxy } from '@nestjs/microservices';
import { firstValueFrom } from 'rxjs';
import { AppRegistrationRequestDTO, AppRegistrationResponseDTO } from './ent-appregistry-app-register.types';

@Injectable()
export class EntAppregistryAppRegisterClient {

  constructor(
    @Inject('NATS_CLIENT') private readonly natsClient: ClientProxy
  ) {
    console.log('Creating events service')
  }

  
    
  async request(event: AppRegistrationRequestDTO): Promise<AppRegistrationResponseDTO> {
    const eventAddress = this.getSubject('ent.appregistry.app.register');
    const reply = await firstValueFrom(this.natsClient.send(eventAddress, event));
    return reply as AppRegistrationResponseDTO;
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
