import { Inject, Injectable } from '@nestjs/common';
import { ClientProxy } from '@nestjs/microservices';
import { firstValueFrom } from 'rxjs';

import type { UpdateGroupResponseDTO } from './types';

import type { AddGroupMemberResponseDTO } from './types';

import type { AppRegistrationRequestDTO } from './types';

import type { FindSessionRequestDTO } from './types';

import type { AppRegistrationResponseDTO } from './types';

import type { AddGroupMemberRequestDTO } from './types';

import type { FindGroupByExternalIdResponseDTO } from './types';

import type { ListenOnlyDTO } from './types';

import type { FetchTranslationsResponseDTO } from './types';

import type { RemoveGroupSharesRequestDTO } from './types';

import type { DeleteGroupRequestDTO } from './types';

import type { UpsertGroupSharesRequestDTO } from './types';

import type { UpdateGroupRequestDTO } from './types';

import type { RemoveGroupMemberRequestDTO } from './types';

import type { RemoveGroupMemberResponseDTO } from './types';

import type { GetUserDisplayNamesRequestDTO } from './types';

import type { RemoveGroupSharesResponseDTO } from './types';

import type { ListenAndAnswerDTO } from './types';

import type { FetchTranslationsRequestDTO } from './types';

import type { UpsertGroupSharesResponseDTO } from './types';

import type { FindGroupByExternalIdRequestDTO } from './types';

import type { RegisterTranslationFilesRequestDTO } from './types';

import type { GetUserDisplayNamesResponseDTO } from './types';

import type { FindSessionResponseDTO } from './types';

import type { CreateGroupRequestDTO } from './types';

import type { CreateGroupResponseDTO } from './types';

import type { RegisterTranslationFilesResponseDTO } from './types';

import type { DeleteGroupResponseDTO } from './types';

import type { DummyResponseDTO } from './types';


@Injectable()
export class EntNatsServiceClient {

  constructor(
    @Inject('NATS_CLIENT') private readonly natsClient: ClientProxy
  ) {
    console.log('Creating events service EntNatsServiceClient')
  }

  
  
  async listenOnlyExample(request: ListenOnlyDTO) {
    const eventAddress = "ent.test.listen";
    this.natsClient.emit(eventAddress, request);
  }
  
  
  
  async listenAndAnswerExample(request: ListenAndAnswerDTO): Promise<DummyResponseDTO> {
    const eventAddress = "ent.test.listen.reply";
    console.debug("Sending request to NATS subject, " + eventAddress);
    const reply = await firstValueFrom(this.natsClient.send(eventAddress, request));
    if(!reply) {
      throw new Error('No reply received');
    }
    return JSON.parse(reply) as DummyResponseDTO;
  }
  
  
  
  async fetchTranslations(request: FetchTranslationsRequestDTO, application: string): Promise<FetchTranslationsResponseDTO> {
    const eventAddress = "i18n." + application + ".fetch";
    console.debug("Sending request to NATS subject, " + eventAddress);
    const reply = await firstValueFrom(this.natsClient.send(eventAddress, request));
    if(!reply) {
      throw new Error('No reply received');
    }
    return JSON.parse(reply) as FetchTranslationsResponseDTO;
  }
  
  
  
  async registerI18nFiles(request: RegisterTranslationFilesRequestDTO, application: string): Promise<RegisterTranslationFilesResponseDTO> {
    const eventAddress = "i18n." + application + ".register";
    console.debug("Sending request to NATS subject, " + eventAddress);
    const reply = await firstValueFrom(this.natsClient.send(eventAddress, request));
    if(!reply) {
      throw new Error('No reply received');
    }
    return JSON.parse(reply) as RegisterTranslationFilesResponseDTO;
  }
  
  
  
  async upsertGroupShares(request: UpsertGroupSharesRequestDTO, application: string): Promise<UpsertGroupSharesResponseDTO> {
    const eventAddress = "share.group.upsert." + application + "";
    console.debug("Sending request to NATS subject, " + eventAddress);
    const reply = await firstValueFrom(this.natsClient.send(eventAddress, request));
    if(!reply) {
      throw new Error('No reply received');
    }
    return JSON.parse(reply) as UpsertGroupSharesResponseDTO;
  }
  
  
  
  async removeGroupShares(request: RemoveGroupSharesRequestDTO, application: string): Promise<RemoveGroupSharesResponseDTO> {
    const eventAddress = "share.group.remove." + application + "";
    console.debug("Sending request to NATS subject, " + eventAddress);
    const reply = await firstValueFrom(this.natsClient.send(eventAddress, request));
    if(!reply) {
      throw new Error('No reply received');
    }
    return JSON.parse(reply) as RemoveGroupSharesResponseDTO;
  }
  
  
  
  async createManualGroup(request: CreateGroupRequestDTO): Promise<CreateGroupResponseDTO> {
    const eventAddress = "directory.group.manual.create";
    console.debug("Sending request to NATS subject, " + eventAddress);
    const reply = await firstValueFrom(this.natsClient.send(eventAddress, request));
    if(!reply) {
      throw new Error('No reply received');
    }
    return JSON.parse(reply) as CreateGroupResponseDTO;
  }
  
  
  
  async updateManualGroup(request: UpdateGroupRequestDTO): Promise<UpdateGroupResponseDTO> {
    const eventAddress = "directory.group.manual.update";
    console.debug("Sending request to NATS subject, " + eventAddress);
    const reply = await firstValueFrom(this.natsClient.send(eventAddress, request));
    if(!reply) {
      throw new Error('No reply received');
    }
    return JSON.parse(reply) as UpdateGroupResponseDTO;
  }
  
  
  
  async deleteManualGroup(request: DeleteGroupRequestDTO): Promise<DeleteGroupResponseDTO> {
    const eventAddress = "directory.group.manual.delete";
    console.debug("Sending request to NATS subject, " + eventAddress);
    const reply = await firstValueFrom(this.natsClient.send(eventAddress, request));
    if(!reply) {
      throw new Error('No reply received');
    }
    return JSON.parse(reply) as DeleteGroupResponseDTO;
  }
  
  
  
  async addGroupMember(request: AddGroupMemberRequestDTO): Promise<AddGroupMemberResponseDTO> {
    const eventAddress = "directory.group.member.add";
    console.debug("Sending request to NATS subject, " + eventAddress);
    const reply = await firstValueFrom(this.natsClient.send(eventAddress, request));
    if(!reply) {
      throw new Error('No reply received');
    }
    return JSON.parse(reply) as AddGroupMemberResponseDTO;
  }
  
  
  
  async removeGroupMember(request: RemoveGroupMemberRequestDTO): Promise<RemoveGroupMemberResponseDTO> {
    const eventAddress = "directory.group.member.delete";
    console.debug("Sending request to NATS subject, " + eventAddress);
    const reply = await firstValueFrom(this.natsClient.send(eventAddress, request));
    if(!reply) {
      throw new Error('No reply received');
    }
    return JSON.parse(reply) as RemoveGroupMemberResponseDTO;
  }
  
  
  
  async findGroupByExternalId(request: FindGroupByExternalIdRequestDTO): Promise<FindGroupByExternalIdResponseDTO> {
    const eventAddress = "directory.group.find.byexternalid";
    console.debug("Sending request to NATS subject, " + eventAddress);
    const reply = await firstValueFrom(this.natsClient.send(eventAddress, request));
    if(!reply) {
      throw new Error('No reply received');
    }
    return JSON.parse(reply) as FindGroupByExternalIdResponseDTO;
  }
  
  
  
  async getUserDisplayNames(request: GetUserDisplayNamesRequestDTO): Promise<GetUserDisplayNamesResponseDTO> {
    const eventAddress = "directory.users.get.displaynames";
    console.debug("Sending request to NATS subject, " + eventAddress);
    const reply = await firstValueFrom(this.natsClient.send(eventAddress, request));
    if(!reply) {
      throw new Error('No reply received');
    }
    return JSON.parse(reply) as GetUserDisplayNamesResponseDTO;
  }
  
  
  
  async findSession(request: FindSessionRequestDTO): Promise<FindSessionResponseDTO> {
    const eventAddress = "session.find";
    console.debug("Sending request to NATS subject, " + eventAddress);
    const reply = await firstValueFrom(this.natsClient.send(eventAddress, request));
    if(!reply) {
      throw new Error('No reply received');
    }
    return JSON.parse(reply) as FindSessionResponseDTO;
  }
  
  
  
  async registerApp(request: AppRegistrationRequestDTO): Promise<AppRegistrationResponseDTO> {
    const eventAddress = "ent.appregistry.app.register";
    console.debug("Sending request to NATS subject, " + eventAddress);
    const reply = await firstValueFrom(this.natsClient.send(eventAddress, request));
    if(!reply) {
      throw new Error('No reply received');
    }
    return JSON.parse(reply) as AppRegistrationResponseDTO;
  }
  
  
  
  async testApplication(request: AppRegistrationRequestDTO, application: string): Promise<AppRegistrationResponseDTO> {
    const eventAddress = "ent." + application + ".test";
    console.debug("Sending request to NATS subject, " + eventAddress);
    const reply = await firstValueFrom(this.natsClient.send(eventAddress, request));
    if(!reply) {
      throw new Error('No reply received');
    }
    return JSON.parse(reply) as AppRegistrationResponseDTO;
  }
  
  
    
}
