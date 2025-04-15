import { Inject, Injectable } from '@nestjs/common';
import { ClientProxy } from '@nestjs/microservices';
import { firstValueFrom } from 'rxjs';
import { UpsertGroupSharesRequestDTO, UpsertGroupSharesResponseDTO } from './share-group-upsert.types';

@Injectable()
export class ShareGroupUpsertClient {

  constructor(
    @Inject('NATS_CLIENT') private readonly natsClient: ClientProxy
  ) {
    console.log('Creating events service')
  }

  
    
  async request(event: UpsertGroupSharesRequestDTO): Promise<UpsertGroupSharesResponseDTO> {
    const eventAddress = this.getSubject('share.group.upsert');
    const reply = await firstValueFrom(this.natsClient.send(eventAddress, event));
    return reply as UpsertGroupSharesResponseDTO;
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
