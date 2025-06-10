package org.entcore.broker;

import io.nats.client.Connection;
import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.List;

@ApplicationScoped
public class EntNatsServiceClient {

  @Inject
  Connection connection;
  @Inject
  ObjectMapper objectMapper;

  public EntNatsServiceClient() {
    Log.info("Registering EntNatsServiceClient");
  }
  
  
  public org.entcore.broker.api.dto.directory.AddGroupMemberResponseDTO addGroupMember(org.entcore.broker.api.dto.directory.AddGroupMemberRequestDTO request) {
    String subject = "directory.group.member.add";
    Log.debug("Sending request to NATS subject: " + subject);
    try {
      final byte[] payload = this.objectMapper.writeValueAsBytes(request);
      
      byte[] response = connection.request(subject, payload).get().getData();
      return this.objectMapper.readValue(response, org.entcore.broker.api.dto.directory.AddGroupMemberResponseDTO.class);
      
    } catch (Exception e) {
      Log.error("Failed to send request to NATS", e);
      throw new RuntimeException("Failed to send request to NATS", e);
    }
  }
  
  public org.entcore.broker.api.dto.directory.CreateGroupResponseDTO createManualGroup(org.entcore.broker.api.dto.directory.CreateGroupRequestDTO request) {
    String subject = "directory.group.manual.create";
    Log.debug("Sending request to NATS subject: " + subject);
    try {
      final byte[] payload = this.objectMapper.writeValueAsBytes(request);
      
      byte[] response = connection.request(subject, payload).get().getData();
      return this.objectMapper.readValue(response, org.entcore.broker.api.dto.directory.CreateGroupResponseDTO.class);
      
    } catch (Exception e) {
      Log.error("Failed to send request to NATS", e);
      throw new RuntimeException("Failed to send request to NATS", e);
    }
  }
  
  public org.entcore.broker.api.dto.directory.DeleteGroupResponseDTO deleteManualGroup(org.entcore.broker.api.dto.directory.DeleteGroupRequestDTO request) {
    String subject = "directory.group.manual.delete";
    Log.debug("Sending request to NATS subject: " + subject);
    try {
      final byte[] payload = this.objectMapper.writeValueAsBytes(request);
      
      byte[] response = connection.request(subject, payload).get().getData();
      return this.objectMapper.readValue(response, org.entcore.broker.api.dto.directory.DeleteGroupResponseDTO.class);
      
    } catch (Exception e) {
      Log.error("Failed to send request to NATS", e);
      throw new RuntimeException("Failed to send request to NATS", e);
    }
  }
  
  public org.entcore.broker.api.dto.i18n.FetchTranslationsResponseDTO fetchTranslations(org.entcore.broker.api.dto.i18n.FetchTranslationsRequestDTO request, final String application) {
    String subject = "i18n." + application + ".fetch";
    Log.debug("Sending request to NATS subject: " + subject);
    try {
      final byte[] payload = this.objectMapper.writeValueAsBytes(request);
      
      byte[] response = connection.request(subject, payload).get().getData();
      return this.objectMapper.readValue(response, org.entcore.broker.api.dto.i18n.FetchTranslationsResponseDTO.class);
      
    } catch (Exception e) {
      Log.error("Failed to send request to NATS", e);
      throw new RuntimeException("Failed to send request to NATS", e);
    }
  }
  
  public org.entcore.broker.api.dto.directory.FindGroupByExternalIdResponseDTO findGroupByExternalId(org.entcore.broker.api.dto.directory.FindGroupByExternalIdRequestDTO request) {
    String subject = "directory.group.find.byexternalid";
    Log.debug("Sending request to NATS subject: " + subject);
    try {
      final byte[] payload = this.objectMapper.writeValueAsBytes(request);
      
      byte[] response = connection.request(subject, payload).get().getData();
      return this.objectMapper.readValue(response, org.entcore.broker.api.dto.directory.FindGroupByExternalIdResponseDTO.class);
      
    } catch (Exception e) {
      Log.error("Failed to send request to NATS", e);
      throw new RuntimeException("Failed to send request to NATS", e);
    }
  }
  
  public org.entcore.broker.api.dto.session.FindSessionResponseDTO findSession(org.entcore.broker.api.dto.session.FindSessionRequestDTO request) {
    String subject = "session.find";
    Log.debug("Sending request to NATS subject: " + subject);
    try {
      final byte[] payload = this.objectMapper.writeValueAsBytes(request);
      
      byte[] response = connection.request(subject, payload).get().getData();
      return this.objectMapper.readValue(response, org.entcore.broker.api.dto.session.FindSessionResponseDTO.class);
      
    } catch (Exception e) {
      Log.error("Failed to send request to NATS", e);
      throw new RuntimeException("Failed to send request to NATS", e);
    }
  }
  
  public org.entcore.broker.api.dto.directory.GetUserDisplayNamesResponseDTO getUserDisplayNames(org.entcore.broker.api.dto.directory.GetUserDisplayNamesRequestDTO request) {
    String subject = "directory.users.get.displaynames";
    Log.debug("Sending request to NATS subject: " + subject);
    try {
      final byte[] payload = this.objectMapper.writeValueAsBytes(request);
      
      byte[] response = connection.request(subject, payload).get().getData();
      return this.objectMapper.readValue(response, org.entcore.broker.api.dto.directory.GetUserDisplayNamesResponseDTO.class);
      
    } catch (Exception e) {
      Log.error("Failed to send request to NATS", e);
      throw new RuntimeException("Failed to send request to NATS", e);
    }
  }
  
  public org.entcore.broker.api.dto.directory.GetUsersByIdsResponseDTO getUsersByIds(org.entcore.broker.api.dto.directory.GetUsersByIdsRequestDTO request) {
    String subject = "directory.users.get.byids";
    Log.debug("Sending request to NATS subject: " + subject);
    try {
      final byte[] payload = this.objectMapper.writeValueAsBytes(request);
      
      byte[] response = connection.request(subject, payload).get().getData();
      return this.objectMapper.readValue(response, org.entcore.broker.api.dto.directory.GetUsersByIdsResponseDTO.class);
      
    } catch (Exception e) {
      Log.error("Failed to send request to NATS", e);
      throw new RuntimeException("Failed to send request to NATS", e);
    }
  }
  
  public org.entcore.broker.api.dto.DummyResponseDTO listenAndReplyExample(org.entcore.broker.api.dto.ListenAndAnswerDTO request) {
    String subject = "ent.test.listen.reply";
    Log.debug("Sending request to NATS subject: " + subject);
    try {
      final byte[] payload = this.objectMapper.writeValueAsBytes(request);
      
      byte[] response = connection.request(subject, payload).get().getData();
      return this.objectMapper.readValue(response, org.entcore.broker.api.dto.DummyResponseDTO.class);
      
    } catch (Exception e) {
      Log.error("Failed to send request to NATS", e);
      throw new RuntimeException("Failed to send request to NATS", e);
    }
  }
  
  public void listenOnlyExample(org.entcore.broker.api.dto.ListenOnlyDTO request) {
    String subject = "ent.test.listen";
    Log.debug("Sending request to NATS subject: " + subject);
    try {
      final byte[] payload = this.objectMapper.writeValueAsBytes(request);
      
      this.connection.publish(subject, payload);
      return;
      
    } catch (Exception e) {
      Log.error("Failed to send request to NATS", e);
      throw new RuntimeException("Failed to send request to NATS", e);
    }
  }
  
  public org.entcore.broker.api.appregistry.AppRegistrationResponseDTO registerApp(org.entcore.broker.api.appregistry.AppRegistrationRequestDTO request) {
    String subject = "ent.appregistry.app.register";
    Log.debug("Sending request to NATS subject: " + subject);
    try {
      final byte[] payload = this.objectMapper.writeValueAsBytes(request);
      
      byte[] response = connection.request(subject, payload).get().getData();
      return this.objectMapper.readValue(response, org.entcore.broker.api.appregistry.AppRegistrationResponseDTO.class);
      
    } catch (Exception e) {
      Log.error("Failed to send request to NATS", e);
      throw new RuntimeException("Failed to send request to NATS", e);
    }
  }
  
  public org.entcore.broker.api.dto.i18n.RegisterTranslationFilesResponseDTO registerI18nFiles(org.entcore.broker.api.dto.i18n.RegisterTranslationFilesRequestDTO request, final String application) {
    String subject = "i18n." + application + ".register";
    Log.debug("Sending request to NATS subject: " + subject);
    try {
      final byte[] payload = this.objectMapper.writeValueAsBytes(request);
      
      byte[] response = connection.request(subject, payload).get().getData();
      return this.objectMapper.readValue(response, org.entcore.broker.api.dto.i18n.RegisterTranslationFilesResponseDTO.class);
      
    } catch (Exception e) {
      Log.error("Failed to send request to NATS", e);
      throw new RuntimeException("Failed to send request to NATS", e);
    }
  }
  
  public org.entcore.broker.api.dto.directory.RemoveGroupMemberResponseDTO removeGroupMember(org.entcore.broker.api.dto.directory.RemoveGroupMemberRequestDTO request) {
    String subject = "directory.group.member.delete";
    Log.debug("Sending request to NATS subject: " + subject);
    try {
      final byte[] payload = this.objectMapper.writeValueAsBytes(request);
      
      byte[] response = connection.request(subject, payload).get().getData();
      return this.objectMapper.readValue(response, org.entcore.broker.api.dto.directory.RemoveGroupMemberResponseDTO.class);
      
    } catch (Exception e) {
      Log.error("Failed to send request to NATS", e);
      throw new RuntimeException("Failed to send request to NATS", e);
    }
  }
  
  public org.entcore.broker.api.dto.shares.RemoveGroupSharesResponseDTO removeGroupShares(org.entcore.broker.api.dto.shares.RemoveGroupSharesRequestDTO request, final String application) {
    String subject = "share.group.remove." + application + "";
    Log.debug("Sending request to NATS subject: " + subject);
    try {
      final byte[] payload = this.objectMapper.writeValueAsBytes(request);
      
      byte[] response = connection.request(subject, payload).get().getData();
      return this.objectMapper.readValue(response, org.entcore.broker.api.dto.shares.RemoveGroupSharesResponseDTO.class);
      
    } catch (Exception e) {
      Log.error("Failed to send request to NATS", e);
      throw new RuntimeException("Failed to send request to NATS", e);
    }
  }
  
  public org.entcore.broker.api.appregistry.AppRegistrationResponseDTO testApplication(org.entcore.broker.api.appregistry.AppRegistrationRequestDTO request, final String application) {
    String subject = "ent." + application + ".test";
    Log.debug("Sending request to NATS subject: " + subject);
    try {
      final byte[] payload = this.objectMapper.writeValueAsBytes(request);
      
      byte[] response = connection.request(subject, payload).get().getData();
      return this.objectMapper.readValue(response, org.entcore.broker.api.appregistry.AppRegistrationResponseDTO.class);
      
    } catch (Exception e) {
      Log.error("Failed to send request to NATS", e);
      throw new RuntimeException("Failed to send request to NATS", e);
    }
  }
  
  public org.entcore.broker.api.dto.directory.UpdateGroupResponseDTO updateManualGroup(org.entcore.broker.api.dto.directory.UpdateGroupRequestDTO request) {
    String subject = "directory.group.manual.update";
    Log.debug("Sending request to NATS subject: " + subject);
    try {
      final byte[] payload = this.objectMapper.writeValueAsBytes(request);
      
      byte[] response = connection.request(subject, payload).get().getData();
      return this.objectMapper.readValue(response, org.entcore.broker.api.dto.directory.UpdateGroupResponseDTO.class);
      
    } catch (Exception e) {
      Log.error("Failed to send request to NATS", e);
      throw new RuntimeException("Failed to send request to NATS", e);
    }
  }
  
  public org.entcore.broker.api.dto.shares.UpsertGroupSharesResponseDTO upsertGroupShares(org.entcore.broker.api.dto.shares.UpsertGroupSharesRequestDTO request, final String application) {
    String subject = "share.group.upsert." + application + "";
    Log.debug("Sending request to NATS subject: " + subject);
    try {
      final byte[] payload = this.objectMapper.writeValueAsBytes(request);
      
      byte[] response = connection.request(subject, payload).get().getData();
      return this.objectMapper.readValue(response, org.entcore.broker.api.dto.shares.UpsertGroupSharesResponseDTO.class);
      
    } catch (Exception e) {
      Log.error("Failed to send request to NATS", e);
      throw new RuntimeException("Failed to send request to NATS", e);
    }
  }
  

}