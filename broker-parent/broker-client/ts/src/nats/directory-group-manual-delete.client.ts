import { Inject, Injectable } from '@nestjs/common';
import { ClientProxy } from '@nestjs/microservices';
import { firstValueFrom } from 'rxjs';
import { DeleteGroupRequestDTO, DeleteGroupResponseDTO } from './directory-group-manual-delete.types';

@Injectable()
export class DirectoryGroupManualDeleteClient {

  constructor(
    @Inject('NATS_CLIENT') private readonly natsClient: ClientProxy
  ) {
    console.log('Creating events service')
  }

  
    
  async request(event: DeleteGroupRequestDTO): Promise<DeleteGroupResponseDTO> {
    const eventAddress = this.getSubject('directory.group.manual.delete');
    const reply = await firstValueFrom(this.natsClient.send(eventAddress, event));
    return reply as DeleteGroupResponseDTO;
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
