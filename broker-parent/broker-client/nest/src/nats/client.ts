/*
 * Copyright © "Edifice"
 *
 * This program is published by "Edifice".
 * You must indicate the name of the software and the company in any production /contribution
 * using the software and indicate on the home page of the software industry in question,
 * "powered by Edifice" with a reference to the website: https://edifice.io/.
 *
 * This program is free software, licensed under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation, version 3 of the License.
 *
 * You can redistribute this application and/or modify it since you respect the terms of the GNU Affero General Public License.
 * If you modify the source code and then use this modified source code in your creation, you must make available the source code of your modifications.
 *
 * You should have received a copy of the GNU Affero General Public License along with the software.
 * If not, please see : <http://www.gnu.org/licenses/>. Full compliance requires reading the terms of this license and following its directives.
 */

import { Injectable } from '@nestjs/common';
import { ClientProxy } from '@nestjs/microservices';
import { firstValueFrom } from 'rxjs';

import type { AddGroupMemberRequestDTO } from './types';

import type { AddGroupMemberResponseDTO } from './types';

import type { AppRegistrationRequestDTO } from './types';

import type { AppRegistrationResponseDTO } from './types';

import type { CreateGroupRequestDTO } from './types';

import type { CreateGroupResponseDTO } from './types';

import type { DeleteGroupRequestDTO } from './types';

import type { DeleteGroupResponseDTO } from './types';

import type { DummyResponseDTO } from './types';

import type { FetchTranslationsRequestDTO } from './types';

import type { FetchTranslationsResponseDTO } from './types';

import type { FindGroupByExternalIdRequestDTO } from './types';

import type { FindGroupByExternalIdResponseDTO } from './types';

import type { FindSessionRequestDTO } from './types';

import type { FindSessionResponseDTO } from './types';

import type { GetUserDisplayNamesRequestDTO } from './types';

import type { GetUserDisplayNamesResponseDTO } from './types';

import type { GetUsersByIdsRequestDTO } from './types';

import type { GetUsersByIdsResponseDTO } from './types';

import type { ListenAndAnswerDTO } from './types';

import type { ListenOnlyDTO } from './types';

import type { RegisterTranslationFilesRequestDTO } from './types';

import type { RegisterTranslationFilesResponseDTO } from './types';

import type { RemoveGroupMemberRequestDTO } from './types';

import type { RemoveGroupMemberResponseDTO } from './types';

import type { RemoveGroupSharesRequestDTO } from './types';

import type { RemoveGroupSharesResponseDTO } from './types';

import type { UpdateGroupRequestDTO } from './types';

import type { UpdateGroupResponseDTO } from './types';

import type { UpsertGroupSharesRequestDTO } from './types';

import type { UpsertGroupSharesResponseDTO } from './types';


@Injectable()
export class EntNatsServiceClient {

  constructor(private readonly natsClient: ClientProxy) {
    console.log('Creating events service EntNatsServiceClient')
  }

  
  
  async addGroupMember(request: AddGroupMemberRequestDTO): Promise<AddGroupMemberResponseDTO> {
    const eventAddress = "directory.group.member.add";
    console.debug("Sending request to NATS subject, " + eventAddress);
    const reply = await firstValueFrom(this.natsClient.send(eventAddress, request));
    if(!reply) {
      throw new Error('No reply received');
    }
    return reply as AddGroupMemberResponseDTO;
  }
  
  
  
  async createManualGroup(request: CreateGroupRequestDTO): Promise<CreateGroupResponseDTO> {
    const eventAddress = "directory.group.manual.create";
    console.debug("Sending request to NATS subject, " + eventAddress);
    const reply = await firstValueFrom(this.natsClient.send(eventAddress, request));
    if(!reply) {
      throw new Error('No reply received');
    }
    return reply as CreateGroupResponseDTO;
  }
  
  
  
  async deleteManualGroup(request: DeleteGroupRequestDTO): Promise<DeleteGroupResponseDTO> {
    const eventAddress = "directory.group.manual.delete";
    console.debug("Sending request to NATS subject, " + eventAddress);
    const reply = await firstValueFrom(this.natsClient.send(eventAddress, request));
    if(!reply) {
      throw new Error('No reply received');
    }
    return reply as DeleteGroupResponseDTO;
  }
  
  
  
  async fetchTranslations(request: FetchTranslationsRequestDTO, application: string): Promise<FetchTranslationsResponseDTO> {
    const eventAddress = "i18n." + application + ".fetch";
    console.debug("Sending request to NATS subject, " + eventAddress);
    const reply = await firstValueFrom(this.natsClient.send(eventAddress, request));
    if(!reply) {
      throw new Error('No reply received');
    }
    return reply as FetchTranslationsResponseDTO;
  }
  
  
  
  async findGroupByExternalId(request: FindGroupByExternalIdRequestDTO): Promise<FindGroupByExternalIdResponseDTO> {
    const eventAddress = "directory.group.find.byexternalid";
    console.debug("Sending request to NATS subject, " + eventAddress);
    const reply = await firstValueFrom(this.natsClient.send(eventAddress, request));
    if(!reply) {
      throw new Error('No reply received');
    }
    return reply as FindGroupByExternalIdResponseDTO;
  }
  
  
  
  async findSession(request: FindSessionRequestDTO): Promise<FindSessionResponseDTO> {
    const eventAddress = "session.find";
    console.debug("Sending request to NATS subject, " + eventAddress);
    const reply = await firstValueFrom(this.natsClient.send(eventAddress, request));
    if(!reply) {
      throw new Error('No reply received');
    }
    return reply as FindSessionResponseDTO;
  }
  
  
  
  async getUserDisplayNames(request: GetUserDisplayNamesRequestDTO): Promise<GetUserDisplayNamesResponseDTO> {
    const eventAddress = "directory.users.get.displaynames";
    console.debug("Sending request to NATS subject, " + eventAddress);
    const reply = await firstValueFrom(this.natsClient.send(eventAddress, request));
    if(!reply) {
      throw new Error('No reply received');
    }
    return reply as GetUserDisplayNamesResponseDTO;
  }
  
  
  
  async getUsersByIds(request: GetUsersByIdsRequestDTO): Promise<GetUsersByIdsResponseDTO> {
    const eventAddress = "directory.users.get.byids";
    console.debug("Sending request to NATS subject, " + eventAddress);
    const reply = await firstValueFrom(this.natsClient.send(eventAddress, request));
    if(!reply) {
      throw new Error('No reply received');
    }
    return reply as GetUsersByIdsResponseDTO;
  }
  
  
  
  async listenAndReplyExample(request: ListenAndAnswerDTO): Promise<DummyResponseDTO> {
    const eventAddress = "ent.test.listen.reply";
    console.debug("Sending request to NATS subject, " + eventAddress);
    const reply = await firstValueFrom(this.natsClient.send(eventAddress, request));
    if(!reply) {
      throw new Error('No reply received');
    }
    return reply as DummyResponseDTO;
  }
  
  
  
  async listenOnlyExample(request: ListenOnlyDTO) {
    const eventAddress = "ent.test.listen";
    this.natsClient.emit(eventAddress, request);
  }
  
  
  
  async registerApp(request: AppRegistrationRequestDTO): Promise<AppRegistrationResponseDTO> {
    const eventAddress = "ent.appregistry.app.register";
    console.debug("Sending request to NATS subject, " + eventAddress);
    const reply = await firstValueFrom(this.natsClient.send(eventAddress, request));
    if(!reply) {
      throw new Error('No reply received');
    }
    return reply as AppRegistrationResponseDTO;
  }
  
  
  
  async registerI18nFiles(request: RegisterTranslationFilesRequestDTO, application: string): Promise<RegisterTranslationFilesResponseDTO> {
    const eventAddress = "i18n." + application + ".register";
    console.debug("Sending request to NATS subject, " + eventAddress);
    const reply = await firstValueFrom(this.natsClient.send(eventAddress, request));
    if(!reply) {
      throw new Error('No reply received');
    }
    return reply as RegisterTranslationFilesResponseDTO;
  }
  
  
  
  async removeGroupMember(request: RemoveGroupMemberRequestDTO): Promise<RemoveGroupMemberResponseDTO> {
    const eventAddress = "directory.group.member.delete";
    console.debug("Sending request to NATS subject, " + eventAddress);
    const reply = await firstValueFrom(this.natsClient.send(eventAddress, request));
    if(!reply) {
      throw new Error('No reply received');
    }
    return reply as RemoveGroupMemberResponseDTO;
  }
  
  
  
  async removeGroupShares(request: RemoveGroupSharesRequestDTO, application: string): Promise<RemoveGroupSharesResponseDTO> {
    const eventAddress = "share.group.remove." + application + "";
    console.debug("Sending request to NATS subject, " + eventAddress);
    const reply = await firstValueFrom(this.natsClient.send(eventAddress, request));
    if(!reply) {
      throw new Error('No reply received');
    }
    return reply as RemoveGroupSharesResponseDTO;
  }
  
  
  
  async testApplication(request: AppRegistrationRequestDTO, application: string): Promise<AppRegistrationResponseDTO> {
    const eventAddress = "ent." + application + ".test";
    console.debug("Sending request to NATS subject, " + eventAddress);
    const reply = await firstValueFrom(this.natsClient.send(eventAddress, request));
    if(!reply) {
      throw new Error('No reply received');
    }
    return reply as AppRegistrationResponseDTO;
  }
  
  
  
  async updateManualGroup(request: UpdateGroupRequestDTO): Promise<UpdateGroupResponseDTO> {
    const eventAddress = "directory.group.manual.update";
    console.debug("Sending request to NATS subject, " + eventAddress);
    const reply = await firstValueFrom(this.natsClient.send(eventAddress, request));
    if(!reply) {
      throw new Error('No reply received');
    }
    return reply as UpdateGroupResponseDTO;
  }
  
  
  
  async upsertGroupShares(request: UpsertGroupSharesRequestDTO, application: string): Promise<UpsertGroupSharesResponseDTO> {
    const eventAddress = "share.group.upsert." + application + "";
    console.debug("Sending request to NATS subject, " + eventAddress);
    const reply = await firstValueFrom(this.natsClient.send(eventAddress, request));
    if(!reply) {
      throw new Error('No reply received');
    }
    return reply as UpsertGroupSharesResponseDTO;
  }
  
  
    
}
