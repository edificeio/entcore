import { Inject, Injectable } from '@nestjs/common';
import { ClientProxy } from '@nestjs/microservices';

import { ListenOnlyDTO,  } from './ent-test-listen.types';

@Injectable()
export class EntTestListenClient {

  constructor(
    @Inject('NATS_CLIENT') private readonly natsClient: ClientProxy
  ) {
    console.log('Creating events service')
  }

    
  async sendEvent(event: ListenOnlyDTO) {
    const eventAddress = this.getSubject('ent.test.listen');
    this.natsClient.emit(eventAddress, event);
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
