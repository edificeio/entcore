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

import type { NatsConnection, Msg } from "@nats-io/nats-core";

import type { AddCommunicationLinksRequestDTO } from './types';

import type { AddCommunicationLinksResponseDTO } from './types';

import type { AddGroupMemberRequestDTO } from './types';

import type { AddGroupMemberResponseDTO } from './types';

import type { AddLinkBetweenGroupsRequestDTO } from './types';

import type { AddLinkBetweenGroupsResponseDTO } from './types';

import type { AppRegistrationRequestDTO } from './types';

import type { AppRegistrationResponseDTO } from './types';

import type { CheckResourceAccessRequestDTO } from './types';

import type { CheckResourceAccessResponseDTO } from './types';

import type { CreateEventRequestDTO } from './types';

import type { CreateEventResponseDTO } from './types';

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

import type { GetResourcesRequestDTO } from './types';

import type { GetResourcesResponseDTO } from './types';

import type { GetUserDisplayNamesRequestDTO } from './types';

import type { GetUserDisplayNamesResponseDTO } from './types';

import type { GetUsersByIdsRequestDTO } from './types';

import type { GetUsersByIdsResponseDTO } from './types';

import type { ListenAndAnswerDTO } from './types';

import type { ListenOnlyDTO } from './types';

import type { LoadTestRequestDTO } from './types';

import type { LoadTestResponseDTO } from './types';

import type { RecreateCommunicationLinksRequestDTO } from './types';

import type { RecreateCommunicationLinksResponseDTO } from './types';

import type { RefreshSessionRequestDTO } from './types';

import type { RefreshSessionResponseDTO } from './types';

import type { RegisterNotificationBatchRequestDTO } from './types';

import type { RegisterNotificationRequestDTO } from './types';

import type { RegisterNotificationResponseDTO } from './types';

import type { RegisterTranslationFilesRequestDTO } from './types';

import type { RegisterTranslationFilesResponseDTO } from './types';

import type { RemoveCommunicationLinksRequestDTO } from './types';

import type { RemoveCommunicationLinksResponseDTO } from './types';

import type { RemoveGroupMemberRequestDTO } from './types';

import type { RemoveGroupMemberResponseDTO } from './types';

import type { RemoveGroupSharesRequestDTO } from './types';

import type { RemoveGroupSharesResponseDTO } from './types';

import type { SendNotificationRequestDTO } from './types';

import type { SendNotificationResponseDTO } from './types';

import type { UpdateGroupRequestDTO } from './types';

import type { UpdateGroupResponseDTO } from './types';

import type { UpsertGroupSharesRequestDTO } from './types';

import type { UpsertGroupSharesResponseDTO } from './types';


export class EntNatsServiceClient {

  constructor(private readonly natsConnection: NatsConnection) {
    console.log('Creating service EntNatsServiceClient')
  }

  
  
  async addCommunicationLinks(request: AddCommunicationLinksRequestDTO): Promise<AddCommunicationLinksResponseDTO> {
    const eventAddress = "communication.link.users.add";
    console.debug("Sending request to NATS subject", {messageAddress: eventAddress});
    const reply: Msg = await this.natsConnection.request(eventAddress, JSON.stringify(request));
    if(!reply) {
      console.warn("No reply received for subject", {messageAddress: eventAddress});
      throw new Error('No reply received');
    }
    return this.extractResponse(reply.json()) as AddCommunicationLinksResponseDTO;
  }
  
  
  
  async addGroupMember(request: AddGroupMemberRequestDTO): Promise<AddGroupMemberResponseDTO> {
    const eventAddress = "directory.group.member.add";
    console.debug("Sending request to NATS subject", {messageAddress: eventAddress});
    const reply: Msg = await this.natsConnection.request(eventAddress, JSON.stringify(request));
    if(!reply) {
      console.warn("No reply received for subject", {messageAddress: eventAddress});
      throw new Error('No reply received');
    }
    return this.extractResponse(reply.json()) as AddGroupMemberResponseDTO;
  }
  
  
  
  async addLinkBetweenGroups(request: AddLinkBetweenGroupsRequestDTO): Promise<AddLinkBetweenGroupsResponseDTO> {
    const eventAddress = "communication.link.groups.add";
    console.debug("Sending request to NATS subject", {messageAddress: eventAddress});
    const reply: Msg = await this.natsConnection.request(eventAddress, JSON.stringify(request));
    if(!reply) {
      console.warn("No reply received for subject", {messageAddress: eventAddress});
      throw new Error('No reply received');
    }
    return this.extractResponse(reply.json()) as AddLinkBetweenGroupsResponseDTO;
  }
  
  
  
  async checkResourceAccess(request: CheckResourceAccessRequestDTO, module: string, resourceType: string): Promise<CheckResourceAccessResponseDTO> {
    const eventAddress = "audience.check.right." + module + "." + resourceType + "";
    console.debug("Sending request to NATS subject", {messageAddress: eventAddress});
    const reply: Msg = await this.natsConnection.request(eventAddress, JSON.stringify(request));
    if(!reply) {
      console.warn("No reply received for subject", {messageAddress: eventAddress});
      throw new Error('No reply received');
    }
    return this.extractResponse(reply.json()) as CheckResourceAccessResponseDTO;
  }
  
  
  
  async createAndStoreEvent(request: CreateEventRequestDTO): Promise<CreateEventResponseDTO> {
    const eventAddress = "event.store.create";
    console.debug("Sending request to NATS subject", {messageAddress: eventAddress});
    const reply: Msg = await this.natsConnection.request(eventAddress, JSON.stringify(request));
    if(!reply) {
      console.warn("No reply received for subject", {messageAddress: eventAddress});
      throw new Error('No reply received');
    }
    return this.extractResponse(reply.json()) as CreateEventResponseDTO;
  }
  
  
  
  async createManualGroup(request: CreateGroupRequestDTO): Promise<CreateGroupResponseDTO> {
    const eventAddress = "directory.group.manual.create";
    console.debug("Sending request to NATS subject", {messageAddress: eventAddress});
    const reply: Msg = await this.natsConnection.request(eventAddress, JSON.stringify(request));
    if(!reply) {
      console.warn("No reply received for subject", {messageAddress: eventAddress});
      throw new Error('No reply received');
    }
    return this.extractResponse(reply.json()) as CreateGroupResponseDTO;
  }
  
  
  
  async deleteManualGroup(request: DeleteGroupRequestDTO): Promise<DeleteGroupResponseDTO> {
    const eventAddress = "directory.group.manual.delete";
    console.debug("Sending request to NATS subject", {messageAddress: eventAddress});
    const reply: Msg = await this.natsConnection.request(eventAddress, JSON.stringify(request));
    if(!reply) {
      console.warn("No reply received for subject", {messageAddress: eventAddress});
      throw new Error('No reply received');
    }
    return this.extractResponse(reply.json()) as DeleteGroupResponseDTO;
  }
  
  
  
  async fetchTranslations(request: FetchTranslationsRequestDTO, application: string): Promise<FetchTranslationsResponseDTO> {
    const eventAddress = "i18n." + application + ".fetch";
    console.debug("Sending request to NATS subject", {messageAddress: eventAddress});
    const reply: Msg = await this.natsConnection.request(eventAddress, JSON.stringify(request));
    if(!reply) {
      console.warn("No reply received for subject", {messageAddress: eventAddress});
      throw new Error('No reply received');
    }
    return this.extractResponse(reply.json()) as FetchTranslationsResponseDTO;
  }
  
  
  
  async findGroupByExternalId(request: FindGroupByExternalIdRequestDTO): Promise<FindGroupByExternalIdResponseDTO> {
    const eventAddress = "directory.group.find.byexternalid";
    console.debug("Sending request to NATS subject", {messageAddress: eventAddress});
    const reply: Msg = await this.natsConnection.request(eventAddress, JSON.stringify(request));
    if(!reply) {
      console.warn("No reply received for subject", {messageAddress: eventAddress});
      throw new Error('No reply received');
    }
    return this.extractResponse(reply.json()) as FindGroupByExternalIdResponseDTO;
  }
  
  
  
  async findSession(request: FindSessionRequestDTO): Promise<FindSessionResponseDTO> {
    const eventAddress = "session.find";
    console.debug("Sending request to NATS subject", {messageAddress: eventAddress});
    const reply: Msg = await this.natsConnection.request(eventAddress, JSON.stringify(request));
    if(!reply) {
      console.warn("No reply received for subject", {messageAddress: eventAddress});
      throw new Error('No reply received');
    }
    return this.extractResponse(reply.json()) as FindSessionResponseDTO;
  }
  
  
  
  async getResources(request: GetResourcesRequestDTO, application: string): Promise<GetResourcesResponseDTO> {
    const eventAddress = "resource.get." + application + "";
    console.debug("Sending request to NATS subject", {messageAddress: eventAddress});
    const reply: Msg = await this.natsConnection.request(eventAddress, JSON.stringify(request));
    if(!reply) {
      console.warn("No reply received for subject", {messageAddress: eventAddress});
      throw new Error('No reply received');
    }
    return this.extractResponse(reply.json()) as GetResourcesResponseDTO;
  }
  
  
  
  async getUserDisplayNames(request: GetUserDisplayNamesRequestDTO): Promise<GetUserDisplayNamesResponseDTO> {
    const eventAddress = "directory.users.get.displaynames";
    console.debug("Sending request to NATS subject", {messageAddress: eventAddress});
    const reply: Msg = await this.natsConnection.request(eventAddress, JSON.stringify(request));
    if(!reply) {
      console.warn("No reply received for subject", {messageAddress: eventAddress});
      throw new Error('No reply received');
    }
    return this.extractResponse(reply.json()) as GetUserDisplayNamesResponseDTO;
  }
  
  
  
  async getUsersByIds(request: GetUsersByIdsRequestDTO): Promise<GetUsersByIdsResponseDTO> {
    const eventAddress = "directory.users.get.byids";
    console.debug("Sending request to NATS subject", {messageAddress: eventAddress});
    const reply: Msg = await this.natsConnection.request(eventAddress, JSON.stringify(request));
    if(!reply) {
      console.warn("No reply received for subject", {messageAddress: eventAddress});
      throw new Error('No reply received');
    }
    return this.extractResponse(reply.json()) as GetUsersByIdsResponseDTO;
  }
  
  
  
  async listenAndReplyExample(request: ListenAndAnswerDTO): Promise<DummyResponseDTO> {
    const eventAddress = "ent.test.listen.reply";
    console.debug("Sending request to NATS subject", {messageAddress: eventAddress});
    const reply: Msg = await this.natsConnection.request(eventAddress, JSON.stringify(request));
    if(!reply) {
      console.warn("No reply received for subject", {messageAddress: eventAddress});
      throw new Error('No reply received');
    }
    return this.extractResponse(reply.json()) as DummyResponseDTO;
  }
  
  
  
  async listenOnlyExample(request: ListenOnlyDTO) {
    const eventAddress = "ent.test.listen";
    console.debug("Publishing to NATS subject", {messageAddress: eventAddress});
    this.natsConnection.publish(eventAddress, JSON.stringify(request));
  }
  
  
  
  async loadTest(request: LoadTestRequestDTO): Promise<LoadTestResponseDTO> {
    const eventAddress = "ent.loadtest";
    console.debug("Sending request to NATS subject", {messageAddress: eventAddress});
    const reply: Msg = await this.natsConnection.request(eventAddress, JSON.stringify(request));
    if(!reply) {
      console.warn("No reply received for subject", {messageAddress: eventAddress});
      throw new Error('No reply received');
    }
    return this.extractResponse(reply.json()) as LoadTestResponseDTO;
  }
  
  
  
  async recreateCommunicationLinks(request: RecreateCommunicationLinksRequestDTO): Promise<RecreateCommunicationLinksResponseDTO> {
    const eventAddress = "communication.link.users.recreate";
    console.debug("Sending request to NATS subject", {messageAddress: eventAddress});
    const reply: Msg = await this.natsConnection.request(eventAddress, JSON.stringify(request));
    if(!reply) {
      console.warn("No reply received for subject", {messageAddress: eventAddress});
      throw new Error('No reply received');
    }
    return this.extractResponse(reply.json()) as RecreateCommunicationLinksResponseDTO;
  }
  
  
  
  async refreshSession(request: RefreshSessionRequestDTO): Promise<RefreshSessionResponseDTO> {
    const eventAddress = "session.refresh";
    console.debug("Sending request to NATS subject", {messageAddress: eventAddress});
    const reply: Msg = await this.natsConnection.request(eventAddress, JSON.stringify(request));
    if(!reply) {
      console.warn("No reply received for subject", {messageAddress: eventAddress});
      throw new Error('No reply received');
    }
    return this.extractResponse(reply.json()) as RefreshSessionResponseDTO;
  }
  
  
  
  async registerApp(request: AppRegistrationRequestDTO): Promise<AppRegistrationResponseDTO> {
    const eventAddress = "ent.appregistry.app.register";
    console.debug("Sending request to NATS subject", {messageAddress: eventAddress});
    const reply: Msg = await this.natsConnection.request(eventAddress, JSON.stringify(request));
    if(!reply) {
      console.warn("No reply received for subject", {messageAddress: eventAddress});
      throw new Error('No reply received');
    }
    return this.extractResponse(reply.json()) as AppRegistrationResponseDTO;
  }
  
  
  
  async registerI18nFiles(request: RegisterTranslationFilesRequestDTO, application: string): Promise<RegisterTranslationFilesResponseDTO> {
    const eventAddress = "i18n." + application + ".register";
    console.debug("Sending request to NATS subject", {messageAddress: eventAddress});
    const reply: Msg = await this.natsConnection.request(eventAddress, JSON.stringify(request));
    if(!reply) {
      console.warn("No reply received for subject", {messageAddress: eventAddress});
      throw new Error('No reply received');
    }
    return this.extractResponse(reply.json()) as RegisterTranslationFilesResponseDTO;
  }
  
  
  
  async registerNotification(request: RegisterNotificationRequestDTO): Promise<RegisterNotificationResponseDTO> {
    const eventAddress = "timeline.notification.register";
    console.debug("Sending request to NATS subject", {messageAddress: eventAddress});
    const reply: Msg = await this.natsConnection.request(eventAddress, JSON.stringify(request));
    if(!reply) {
      console.warn("No reply received for subject", {messageAddress: eventAddress});
      throw new Error('No reply received');
    }
    return this.extractResponse(reply.json()) as RegisterNotificationResponseDTO;
  }
  
  
  
  async registerNotifications(request: RegisterNotificationBatchRequestDTO): Promise<RegisterNotificationResponseDTO> {
    const eventAddress = "timeline.notification.register.batch";
    console.debug("Sending request to NATS subject", {messageAddress: eventAddress});
    const reply: Msg = await this.natsConnection.request(eventAddress, JSON.stringify(request));
    if(!reply) {
      console.warn("No reply received for subject", {messageAddress: eventAddress});
      throw new Error('No reply received');
    }
    return this.extractResponse(reply.json()) as RegisterNotificationResponseDTO;
  }
  
  
  
  async removeCommunicationLinks(request: RemoveCommunicationLinksRequestDTO): Promise<RemoveCommunicationLinksResponseDTO> {
    const eventAddress = "communication.link.users.remove";
    console.debug("Sending request to NATS subject", {messageAddress: eventAddress});
    const reply: Msg = await this.natsConnection.request(eventAddress, JSON.stringify(request));
    if(!reply) {
      console.warn("No reply received for subject", {messageAddress: eventAddress});
      throw new Error('No reply received');
    }
    return this.extractResponse(reply.json()) as RemoveCommunicationLinksResponseDTO;
  }
  
  
  
  async removeGroupMember(request: RemoveGroupMemberRequestDTO): Promise<RemoveGroupMemberResponseDTO> {
    const eventAddress = "directory.group.member.delete";
    console.debug("Sending request to NATS subject", {messageAddress: eventAddress});
    const reply: Msg = await this.natsConnection.request(eventAddress, JSON.stringify(request));
    if(!reply) {
      console.warn("No reply received for subject", {messageAddress: eventAddress});
      throw new Error('No reply received');
    }
    return this.extractResponse(reply.json()) as RemoveGroupMemberResponseDTO;
  }
  
  
  
  async removeGroupShares(request: RemoveGroupSharesRequestDTO, application: string): Promise<RemoveGroupSharesResponseDTO> {
    const eventAddress = "share.group.remove." + application + "";
    console.debug("Sending request to NATS subject", {messageAddress: eventAddress});
    const reply: Msg = await this.natsConnection.request(eventAddress, JSON.stringify(request));
    if(!reply) {
      console.warn("No reply received for subject", {messageAddress: eventAddress});
      throw new Error('No reply received');
    }
    return this.extractResponse(reply.json()) as RemoveGroupSharesResponseDTO;
  }
  
  
  
  async sendNotification(request: SendNotificationRequestDTO): Promise<SendNotificationResponseDTO> {
    const eventAddress = "timeline.notification.send";
    console.debug("Sending request to NATS subject", {messageAddress: eventAddress});
    const reply: Msg = await this.natsConnection.request(eventAddress, JSON.stringify(request));
    if(!reply) {
      console.warn("No reply received for subject", {messageAddress: eventAddress});
      throw new Error('No reply received');
    }
    return this.extractResponse(reply.json()) as SendNotificationResponseDTO;
  }
  
  
  
  async updateManualGroup(request: UpdateGroupRequestDTO): Promise<UpdateGroupResponseDTO> {
    const eventAddress = "directory.group.manual.update";
    console.debug("Sending request to NATS subject", {messageAddress: eventAddress});
    const reply: Msg = await this.natsConnection.request(eventAddress, JSON.stringify(request));
    if(!reply) {
      console.warn("No reply received for subject", {messageAddress: eventAddress});
      throw new Error('No reply received');
    }
    return this.extractResponse(reply.json()) as UpdateGroupResponseDTO;
  }
  
  
  
  async upsertGroupShares(request: UpsertGroupSharesRequestDTO, application: string): Promise<UpsertGroupSharesResponseDTO> {
    const eventAddress = "share.group.upsert." + application + "";
    console.debug("Sending request to NATS subject", {messageAddress: eventAddress});
    const reply: Msg = await this.natsConnection.request(eventAddress, JSON.stringify(request));
    if(!reply) {
      console.warn("No reply received for subject", {messageAddress: eventAddress});
      throw new Error('No reply received');
    }
    return this.extractResponse(reply.json()) as UpsertGroupSharesResponseDTO;
  }
  
  

  private extractResponse(replyData: any): any {
    return replyData.response || replyData;
  }
    
}
