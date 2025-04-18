import { Inject, Injectable } from '@nestjs/common';
import { ClientProxy } from '@nestjs/microservices';
import { firstValueFrom } from 'rxjs';import { FetchTranslationsRequestDTO } from './ent-nats-service.types';
import { UpsertGroupSharesRequestDTO } from './ent-nats-service.types';
import { RemoveGroupSharesRequestDTO } from './ent-nats-service.types';
import { CreateGroupRequestDTO } from './ent-nats-service.types';
import { UpdateGroupRequestDTO } from './ent-nats-service.types';
import { DeleteGroupRequestDTO } from './ent-nats-service.types';
import { AddGroupMemberRequestDTO } from './ent-nats-service.types';
import { RemoveGroupMemberRequestDTO } from './ent-nats-service.types';
import { FindGroupByExternalIdRequestDTO } from './ent-nats-service.types';
import { FindSessionRequestDTO } from './ent-nats-service.types';
import { AppRegistrationRequestDTO } from './ent-nats-service.types';
import { FetchTranslationsResponseDTO } from './ent-nats-service.types';
import { UpsertGroupSharesResponseDTO } from './ent-nats-service.types';
import { RemoveGroupSharesResponseDTO } from './ent-nats-service.types';
import { CreateGroupResponseDTO } from './ent-nats-service.types';
import { UpdateGroupResponseDTO } from './ent-nats-service.types';
import { DeleteGroupResponseDTO } from './ent-nats-service.types';
import { AddGroupMemberResponseDTO } from './ent-nats-service.types';
import { RemoveGroupMemberResponseDTO } from './ent-nats-service.types';
import { FindGroupByExternalIdResponseDTO } from './ent-nats-service.types';
import { FindSessionResponseDTO } from './ent-nats-service.types';
import { AppRegistrationResponseDTO } from './ent-nats-service.types';


@Injectable()
export class EntNatsServiceClient {

  constructor(
    @Inject('NATS_CLIENT') private readonly natsClient: ClientProxy
    , private readonly application: string
  ) {
    console.log('Creating events service EntNatsServiceClient')
  }

    
  async i18nTranslationsFetch(event: FetchTranslationsRequestDTO): Promise<FetchTranslationsResponseDTO> {
    const eventAddress = this.getSubject(`i18n.translations.fetch`);
    const reply = await firstValueFrom(this.natsClient.send(eventAddress, event));
    return reply as FetchTranslationsResponseDTO;
  }
        
  
  async shareGroupUpsertByApplication(event: UpsertGroupSharesRequestDTO): Promise<UpsertGroupSharesResponseDTO> {
    const eventAddress = this.getSubject(`share.group.upsert.${this.application}`);
    const reply = await firstValueFrom(this.natsClient.send(eventAddress, event));
    return reply as UpsertGroupSharesResponseDTO;
  }
        
  
  async shareGroupRemoveByApplication(event: RemoveGroupSharesRequestDTO): Promise<RemoveGroupSharesResponseDTO> {
    const eventAddress = this.getSubject(`share.group.remove.${this.application}`);
    const reply = await firstValueFrom(this.natsClient.send(eventAddress, event));
    return reply as RemoveGroupSharesResponseDTO;
  }
        
  
  async directoryGroupManualCreate(event: CreateGroupRequestDTO): Promise<CreateGroupResponseDTO> {
    const eventAddress = this.getSubject(`directory.group.manual.create`);
    const reply = await firstValueFrom(this.natsClient.send(eventAddress, event));
    return reply as CreateGroupResponseDTO;
  }
        
  
  async directoryGroupManualUpdate(event: UpdateGroupRequestDTO): Promise<UpdateGroupResponseDTO> {
    const eventAddress = this.getSubject(`directory.group.manual.update`);
    const reply = await firstValueFrom(this.natsClient.send(eventAddress, event));
    return reply as UpdateGroupResponseDTO;
  }
        
  
  async directoryGroupManualDelete(event: DeleteGroupRequestDTO): Promise<DeleteGroupResponseDTO> {
    const eventAddress = this.getSubject(`directory.group.manual.delete`);
    const reply = await firstValueFrom(this.natsClient.send(eventAddress, event));
    return reply as DeleteGroupResponseDTO;
  }
        
  
  async directoryGroupMemberAdd(event: AddGroupMemberRequestDTO): Promise<AddGroupMemberResponseDTO> {
    const eventAddress = this.getSubject(`directory.group.member.add`);
    const reply = await firstValueFrom(this.natsClient.send(eventAddress, event));
    return reply as AddGroupMemberResponseDTO;
  }
        
  
  async directoryGroupMemberDelete(event: RemoveGroupMemberRequestDTO): Promise<RemoveGroupMemberResponseDTO> {
    const eventAddress = this.getSubject(`directory.group.member.delete`);
    const reply = await firstValueFrom(this.natsClient.send(eventAddress, event));
    return reply as RemoveGroupMemberResponseDTO;
  }
        
  
  async directoryGroupFindByexternalid(event: FindGroupByExternalIdRequestDTO): Promise<FindGroupByExternalIdResponseDTO> {
    const eventAddress = this.getSubject(`directory.group.find.byexternalid`);
    const reply = await firstValueFrom(this.natsClient.send(eventAddress, event));
    return reply as FindGroupByExternalIdResponseDTO;
  }
        
  
  async sessionFind(event: FindSessionRequestDTO): Promise<FindSessionResponseDTO> {
    const eventAddress = this.getSubject(`session.find`);
    const reply = await firstValueFrom(this.natsClient.send(eventAddress, event));
    return reply as FindSessionResponseDTO;
  }
        
  
  async entAppregistryAppRegister(event: AppRegistrationRequestDTO): Promise<AppRegistrationResponseDTO> {
    const eventAddress = this.getSubject(`ent.appregistry.app.register`);
    const reply = await firstValueFrom(this.natsClient.send(eventAddress, event));
    return reply as AppRegistrationResponseDTO;
  }
        
  
  async entByApplicationTest(event: AppRegistrationRequestDTO): Promise<AppRegistrationResponseDTO> {
    const eventAddress = this.getSubject(`ent.${this.application}.test`);
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
