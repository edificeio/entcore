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

package org.entcore.broker;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.nats.client.Connection;
import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;


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
  
  
  public org.entcore.broker.api.dto.communication.AddCommunicationLinksResponseDTO addCommunicationLinks(org.entcore.broker.api.dto.communication.AddCommunicationLinksRequestDTO request) {
    String subject = "communication.link.users.add";
    Log.debug("Sending request to NATS subject: " + subject);
    try {
      final byte[] payload = this.objectMapper.writeValueAsBytes(request);
      
      final byte[] response = connection.request(subject, payload).get().getData();
      final JsonNode responseTree = this.objectMapper.readTree(response);
      final JsonNode responseNode = responseTree.get("response");
      if(responseNode == null) {
        final JsonNode err = responseTree.get("err");
        if(err == null) {
          Log.error("Response from NATS is null or malformed");
          throw new RuntimeException("Response from NATS is null or malformed but no error provided");
        } else {
          Log.error("An error occurred while requesting subject " + subject + "  " + err.asText());
          throw new RuntimeException(err.asText());
        }
      } else if (!responseNode.isObject()) {
          Log.error("Response is not a valid JSON object: " + responseNode);
          throw new RuntimeException("Response is not a valid JSON object");
      } else {
        return this.objectMapper.treeToValue(responseNode, org.entcore.broker.api.dto.communication.AddCommunicationLinksResponseDTO.class);
      }
      
    } catch (Exception e) {
      Log.error("Failed to send request to NATS", e);
      throw new RuntimeException("Failed to send request to NATS", e);
    }
  }
  
  public org.entcore.broker.api.dto.directory.AddGroupMemberResponseDTO addGroupMember(org.entcore.broker.api.dto.directory.AddGroupMemberRequestDTO request) {
    String subject = "directory.group.member.add";
    Log.debug("Sending request to NATS subject: " + subject);
    try {
      final byte[] payload = this.objectMapper.writeValueAsBytes(request);
      
      final byte[] response = connection.request(subject, payload).get().getData();
      final JsonNode responseTree = this.objectMapper.readTree(response);
      final JsonNode responseNode = responseTree.get("response");
      if(responseNode == null) {
        final JsonNode err = responseTree.get("err");
        if(err == null) {
          Log.error("Response from NATS is null or malformed");
          throw new RuntimeException("Response from NATS is null or malformed but no error provided");
        } else {
          Log.error("An error occurred while requesting subject " + subject + "  " + err.asText());
          throw new RuntimeException(err.asText());
        }
      } else if (!responseNode.isObject()) {
          Log.error("Response is not a valid JSON object: " + responseNode);
          throw new RuntimeException("Response is not a valid JSON object");
      } else {
        return this.objectMapper.treeToValue(responseNode, org.entcore.broker.api.dto.directory.AddGroupMemberResponseDTO.class);
      }
      
    } catch (Exception e) {
      Log.error("Failed to send request to NATS", e);
      throw new RuntimeException("Failed to send request to NATS", e);
    }
  }
  
  public org.entcore.broker.api.dto.communication.AddLinkBetweenGroupsResponseDTO addLinkBetweenGroups(org.entcore.broker.api.dto.communication.AddLinkBetweenGroupsRequestDTO request) {
    String subject = "communication.link.groups.add";
    Log.debug("Sending request to NATS subject: " + subject);
    try {
      final byte[] payload = this.objectMapper.writeValueAsBytes(request);
      
      final byte[] response = connection.request(subject, payload).get().getData();
      final JsonNode responseTree = this.objectMapper.readTree(response);
      final JsonNode responseNode = responseTree.get("response");
      if(responseNode == null) {
        final JsonNode err = responseTree.get("err");
        if(err == null) {
          Log.error("Response from NATS is null or malformed");
          throw new RuntimeException("Response from NATS is null or malformed but no error provided");
        } else {
          Log.error("An error occurred while requesting subject " + subject + "  " + err.asText());
          throw new RuntimeException(err.asText());
        }
      } else if (!responseNode.isObject()) {
          Log.error("Response is not a valid JSON object: " + responseNode);
          throw new RuntimeException("Response is not a valid JSON object");
      } else {
        return this.objectMapper.treeToValue(responseNode, org.entcore.broker.api.dto.communication.AddLinkBetweenGroupsResponseDTO.class);
      }
      
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
      
      final byte[] response = connection.request(subject, payload).get().getData();
      final JsonNode responseTree = this.objectMapper.readTree(response);
      final JsonNode responseNode = responseTree.get("response");
      if(responseNode == null) {
        final JsonNode err = responseTree.get("err");
        if(err == null) {
          Log.error("Response from NATS is null or malformed");
          throw new RuntimeException("Response from NATS is null or malformed but no error provided");
        } else {
          Log.error("An error occurred while requesting subject " + subject + "  " + err.asText());
          throw new RuntimeException(err.asText());
        }
      } else if (!responseNode.isObject()) {
          Log.error("Response is not a valid JSON object: " + responseNode);
          throw new RuntimeException("Response is not a valid JSON object");
      } else {
        return this.objectMapper.treeToValue(responseNode, org.entcore.broker.api.dto.directory.CreateGroupResponseDTO.class);
      }
      
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
      
      final byte[] response = connection.request(subject, payload).get().getData();
      final JsonNode responseTree = this.objectMapper.readTree(response);
      final JsonNode responseNode = responseTree.get("response");
      if(responseNode == null) {
        final JsonNode err = responseTree.get("err");
        if(err == null) {
          Log.error("Response from NATS is null or malformed");
          throw new RuntimeException("Response from NATS is null or malformed but no error provided");
        } else {
          Log.error("An error occurred while requesting subject " + subject + "  " + err.asText());
          throw new RuntimeException(err.asText());
        }
      } else if (!responseNode.isObject()) {
          Log.error("Response is not a valid JSON object: " + responseNode);
          throw new RuntimeException("Response is not a valid JSON object");
      } else {
        return this.objectMapper.treeToValue(responseNode, org.entcore.broker.api.dto.directory.DeleteGroupResponseDTO.class);
      }
      
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
      
      final byte[] response = connection.request(subject, payload).get().getData();
      final JsonNode responseTree = this.objectMapper.readTree(response);
      final JsonNode responseNode = responseTree.get("response");
      if(responseNode == null) {
        final JsonNode err = responseTree.get("err");
        if(err == null) {
          Log.error("Response from NATS is null or malformed");
          throw new RuntimeException("Response from NATS is null or malformed but no error provided");
        } else {
          Log.error("An error occurred while requesting subject " + subject + "  " + err.asText());
          throw new RuntimeException(err.asText());
        }
      } else if (!responseNode.isObject()) {
          Log.error("Response is not a valid JSON object: " + responseNode);
          throw new RuntimeException("Response is not a valid JSON object");
      } else {
        return this.objectMapper.treeToValue(responseNode, org.entcore.broker.api.dto.i18n.FetchTranslationsResponseDTO.class);
      }
      
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
      
      final byte[] response = connection.request(subject, payload).get().getData();
      final JsonNode responseTree = this.objectMapper.readTree(response);
      final JsonNode responseNode = responseTree.get("response");
      if(responseNode == null) {
        final JsonNode err = responseTree.get("err");
        if(err == null) {
          Log.error("Response from NATS is null or malformed");
          throw new RuntimeException("Response from NATS is null or malformed but no error provided");
        } else {
          Log.error("An error occurred while requesting subject " + subject + "  " + err.asText());
          throw new RuntimeException(err.asText());
        }
      } else if (!responseNode.isObject()) {
          Log.error("Response is not a valid JSON object: " + responseNode);
          throw new RuntimeException("Response is not a valid JSON object");
      } else {
        return this.objectMapper.treeToValue(responseNode, org.entcore.broker.api.dto.directory.FindGroupByExternalIdResponseDTO.class);
      }
      
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
      
      final byte[] response = connection.request(subject, payload).get().getData();
      final JsonNode responseTree = this.objectMapper.readTree(response);
      final JsonNode responseNode = responseTree.get("response");
      if(responseNode == null) {
        final JsonNode err = responseTree.get("err");
        if(err == null) {
          Log.error("Response from NATS is null or malformed");
          throw new RuntimeException("Response from NATS is null or malformed but no error provided");
        } else {
          Log.error("An error occurred while requesting subject " + subject + "  " + err.asText());
          throw new RuntimeException(err.asText());
        }
      } else if (!responseNode.isObject()) {
          Log.error("Response is not a valid JSON object: " + responseNode);
          throw new RuntimeException("Response is not a valid JSON object");
      } else {
        return this.objectMapper.treeToValue(responseNode, org.entcore.broker.api.dto.session.FindSessionResponseDTO.class);
      }
      
    } catch (Exception e) {
      Log.error("Failed to send request to NATS", e);
      throw new RuntimeException("Failed to send request to NATS", e);
    }
  }
  
  public org.entcore.broker.api.dto.resources.GetResourcesResponseDTO getResources(org.entcore.broker.api.dto.resources.GetResourcesRequestDTO request, final String application) {
    String subject = "resource.get." + application + "";
    Log.debug("Sending request to NATS subject: " + subject);
    try {
      final byte[] payload = this.objectMapper.writeValueAsBytes(request);
      
      final byte[] response = connection.request(subject, payload).get().getData();
      final JsonNode responseTree = this.objectMapper.readTree(response);
      final JsonNode responseNode = responseTree.get("response");
      if(responseNode == null) {
        final JsonNode err = responseTree.get("err");
        if(err == null) {
          Log.error("Response from NATS is null or malformed");
          throw new RuntimeException("Response from NATS is null or malformed but no error provided");
        } else {
          Log.error("An error occurred while requesting subject " + subject + "  " + err.asText());
          throw new RuntimeException(err.asText());
        }
      } else if (!responseNode.isObject()) {
          Log.error("Response is not a valid JSON object: " + responseNode);
          throw new RuntimeException("Response is not a valid JSON object");
      } else {
        return this.objectMapper.treeToValue(responseNode, org.entcore.broker.api.dto.resources.GetResourcesResponseDTO.class);
      }
      
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
      
      final byte[] response = connection.request(subject, payload).get().getData();
      final JsonNode responseTree = this.objectMapper.readTree(response);
      final JsonNode responseNode = responseTree.get("response");
      if(responseNode == null) {
        final JsonNode err = responseTree.get("err");
        if(err == null) {
          Log.error("Response from NATS is null or malformed");
          throw new RuntimeException("Response from NATS is null or malformed but no error provided");
        } else {
          Log.error("An error occurred while requesting subject " + subject + "  " + err.asText());
          throw new RuntimeException(err.asText());
        }
      } else if (!responseNode.isObject()) {
          Log.error("Response is not a valid JSON object: " + responseNode);
          throw new RuntimeException("Response is not a valid JSON object");
      } else {
        return this.objectMapper.treeToValue(responseNode, org.entcore.broker.api.dto.directory.GetUserDisplayNamesResponseDTO.class);
      }
      
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
      
      final byte[] response = connection.request(subject, payload).get().getData();
      final JsonNode responseTree = this.objectMapper.readTree(response);
      final JsonNode responseNode = responseTree.get("response");
      if(responseNode == null) {
        final JsonNode err = responseTree.get("err");
        if(err == null) {
          Log.error("Response from NATS is null or malformed");
          throw new RuntimeException("Response from NATS is null or malformed but no error provided");
        } else {
          Log.error("An error occurred while requesting subject " + subject + "  " + err.asText());
          throw new RuntimeException(err.asText());
        }
      } else if (!responseNode.isObject()) {
          Log.error("Response is not a valid JSON object: " + responseNode);
          throw new RuntimeException("Response is not a valid JSON object");
      } else {
        return this.objectMapper.treeToValue(responseNode, org.entcore.broker.api.dto.directory.GetUsersByIdsResponseDTO.class);
      }
      
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
      
      final byte[] response = connection.request(subject, payload).get().getData();
      final JsonNode responseTree = this.objectMapper.readTree(response);
      final JsonNode responseNode = responseTree.get("response");
      if(responseNode == null) {
        final JsonNode err = responseTree.get("err");
        if(err == null) {
          Log.error("Response from NATS is null or malformed");
          throw new RuntimeException("Response from NATS is null or malformed but no error provided");
        } else {
          Log.error("An error occurred while requesting subject " + subject + "  " + err.asText());
          throw new RuntimeException(err.asText());
        }
      } else if (!responseNode.isObject()) {
          Log.error("Response is not a valid JSON object: " + responseNode);
          throw new RuntimeException("Response is not a valid JSON object");
      } else {
        return this.objectMapper.treeToValue(responseNode, org.entcore.broker.api.dto.DummyResponseDTO.class);
      }
      
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
  
  public org.entcore.broker.api.dto.communication.RecreateCommunicationLinksResponseDTO recreateCommunicationLinks(org.entcore.broker.api.dto.communication.RecreateCommunicationLinksRequestDTO request) {
    String subject = "communication.link.users.recreate";
    Log.debug("Sending request to NATS subject: " + subject);
    try {
      final byte[] payload = this.objectMapper.writeValueAsBytes(request);
      
      final byte[] response = connection.request(subject, payload).get().getData();
      final JsonNode responseTree = this.objectMapper.readTree(response);
      final JsonNode responseNode = responseTree.get("response");
      if(responseNode == null) {
        final JsonNode err = responseTree.get("err");
        if(err == null) {
          Log.error("Response from NATS is null or malformed");
          throw new RuntimeException("Response from NATS is null or malformed but no error provided");
        } else {
          Log.error("An error occurred while requesting subject " + subject + "  " + err.asText());
          throw new RuntimeException(err.asText());
        }
      } else if (!responseNode.isObject()) {
          Log.error("Response is not a valid JSON object: " + responseNode);
          throw new RuntimeException("Response is not a valid JSON object");
      } else {
        return this.objectMapper.treeToValue(responseNode, org.entcore.broker.api.dto.communication.RecreateCommunicationLinksResponseDTO.class);
      }
      
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
      
      final byte[] response = connection.request(subject, payload).get().getData();
      final JsonNode responseTree = this.objectMapper.readTree(response);
      final JsonNode responseNode = responseTree.get("response");
      if(responseNode == null) {
        final JsonNode err = responseTree.get("err");
        if(err == null) {
          Log.error("Response from NATS is null or malformed");
          throw new RuntimeException("Response from NATS is null or malformed but no error provided");
        } else {
          Log.error("An error occurred while requesting subject " + subject + "  " + err.asText());
          throw new RuntimeException(err.asText());
        }
      } else if (!responseNode.isObject()) {
          Log.error("Response is not a valid JSON object: " + responseNode);
          throw new RuntimeException("Response is not a valid JSON object");
      } else {
        return this.objectMapper.treeToValue(responseNode, org.entcore.broker.api.appregistry.AppRegistrationResponseDTO.class);
      }
      
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
      
      final byte[] response = connection.request(subject, payload).get().getData();
      final JsonNode responseTree = this.objectMapper.readTree(response);
      final JsonNode responseNode = responseTree.get("response");
      if(responseNode == null) {
        final JsonNode err = responseTree.get("err");
        if(err == null) {
          Log.error("Response from NATS is null or malformed");
          throw new RuntimeException("Response from NATS is null or malformed but no error provided");
        } else {
          Log.error("An error occurred while requesting subject " + subject + "  " + err.asText());
          throw new RuntimeException(err.asText());
        }
      } else if (!responseNode.isObject()) {
          Log.error("Response is not a valid JSON object: " + responseNode);
          throw new RuntimeException("Response is not a valid JSON object");
      } else {
        return this.objectMapper.treeToValue(responseNode, org.entcore.broker.api.dto.i18n.RegisterTranslationFilesResponseDTO.class);
      }
      
    } catch (Exception e) {
      Log.error("Failed to send request to NATS", e);
      throw new RuntimeException("Failed to send request to NATS", e);
    }
  }
  
  public org.entcore.broker.api.dto.communication.RemoveCommunicationLinksResponseDTO removeCommunicationLinks(org.entcore.broker.api.dto.communication.RemoveCommunicationLinksRequestDTO request) {
    String subject = "communication.link.users.remove";
    Log.debug("Sending request to NATS subject: " + subject);
    try {
      final byte[] payload = this.objectMapper.writeValueAsBytes(request);
      
      final byte[] response = connection.request(subject, payload).get().getData();
      final JsonNode responseTree = this.objectMapper.readTree(response);
      final JsonNode responseNode = responseTree.get("response");
      if(responseNode == null) {
        final JsonNode err = responseTree.get("err");
        if(err == null) {
          Log.error("Response from NATS is null or malformed");
          throw new RuntimeException("Response from NATS is null or malformed but no error provided");
        } else {
          Log.error("An error occurred while requesting subject " + subject + "  " + err.asText());
          throw new RuntimeException(err.asText());
        }
      } else if (!responseNode.isObject()) {
          Log.error("Response is not a valid JSON object: " + responseNode);
          throw new RuntimeException("Response is not a valid JSON object");
      } else {
        return this.objectMapper.treeToValue(responseNode, org.entcore.broker.api.dto.communication.RemoveCommunicationLinksResponseDTO.class);
      }
      
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
      
      final byte[] response = connection.request(subject, payload).get().getData();
      final JsonNode responseTree = this.objectMapper.readTree(response);
      final JsonNode responseNode = responseTree.get("response");
      if(responseNode == null) {
        final JsonNode err = responseTree.get("err");
        if(err == null) {
          Log.error("Response from NATS is null or malformed");
          throw new RuntimeException("Response from NATS is null or malformed but no error provided");
        } else {
          Log.error("An error occurred while requesting subject " + subject + "  " + err.asText());
          throw new RuntimeException(err.asText());
        }
      } else if (!responseNode.isObject()) {
          Log.error("Response is not a valid JSON object: " + responseNode);
          throw new RuntimeException("Response is not a valid JSON object");
      } else {
        return this.objectMapper.treeToValue(responseNode, org.entcore.broker.api.dto.directory.RemoveGroupMemberResponseDTO.class);
      }
      
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
      
      final byte[] response = connection.request(subject, payload).get().getData();
      final JsonNode responseTree = this.objectMapper.readTree(response);
      final JsonNode responseNode = responseTree.get("response");
      if(responseNode == null) {
        final JsonNode err = responseTree.get("err");
        if(err == null) {
          Log.error("Response from NATS is null or malformed");
          throw new RuntimeException("Response from NATS is null or malformed but no error provided");
        } else {
          Log.error("An error occurred while requesting subject " + subject + "  " + err.asText());
          throw new RuntimeException(err.asText());
        }
      } else if (!responseNode.isObject()) {
          Log.error("Response is not a valid JSON object: " + responseNode);
          throw new RuntimeException("Response is not a valid JSON object");
      } else {
        return this.objectMapper.treeToValue(responseNode, org.entcore.broker.api.dto.shares.RemoveGroupSharesResponseDTO.class);
      }
      
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
      
      final byte[] response = connection.request(subject, payload).get().getData();
      final JsonNode responseTree = this.objectMapper.readTree(response);
      final JsonNode responseNode = responseTree.get("response");
      if(responseNode == null) {
        final JsonNode err = responseTree.get("err");
        if(err == null) {
          Log.error("Response from NATS is null or malformed");
          throw new RuntimeException("Response from NATS is null or malformed but no error provided");
        } else {
          Log.error("An error occurred while requesting subject " + subject + "  " + err.asText());
          throw new RuntimeException(err.asText());
        }
      } else if (!responseNode.isObject()) {
          Log.error("Response is not a valid JSON object: " + responseNode);
          throw new RuntimeException("Response is not a valid JSON object");
      } else {
        return this.objectMapper.treeToValue(responseNode, org.entcore.broker.api.appregistry.AppRegistrationResponseDTO.class);
      }
      
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
      
      final byte[] response = connection.request(subject, payload).get().getData();
      final JsonNode responseTree = this.objectMapper.readTree(response);
      final JsonNode responseNode = responseTree.get("response");
      if(responseNode == null) {
        final JsonNode err = responseTree.get("err");
        if(err == null) {
          Log.error("Response from NATS is null or malformed");
          throw new RuntimeException("Response from NATS is null or malformed but no error provided");
        } else {
          Log.error("An error occurred while requesting subject " + subject + "  " + err.asText());
          throw new RuntimeException(err.asText());
        }
      } else if (!responseNode.isObject()) {
          Log.error("Response is not a valid JSON object: " + responseNode);
          throw new RuntimeException("Response is not a valid JSON object");
      } else {
        return this.objectMapper.treeToValue(responseNode, org.entcore.broker.api.dto.directory.UpdateGroupResponseDTO.class);
      }
      
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
      
      final byte[] response = connection.request(subject, payload).get().getData();
      final JsonNode responseTree = this.objectMapper.readTree(response);
      final JsonNode responseNode = responseTree.get("response");
      if(responseNode == null) {
        final JsonNode err = responseTree.get("err");
        if(err == null) {
          Log.error("Response from NATS is null or malformed");
          throw new RuntimeException("Response from NATS is null or malformed but no error provided");
        } else {
          Log.error("An error occurred while requesting subject " + subject + "  " + err.asText());
          throw new RuntimeException(err.asText());
        }
      } else if (!responseNode.isObject()) {
          Log.error("Response is not a valid JSON object: " + responseNode);
          throw new RuntimeException("Response is not a valid JSON object");
      } else {
        return this.objectMapper.treeToValue(responseNode, org.entcore.broker.api.dto.shares.UpsertGroupSharesResponseDTO.class);
      }
      
    } catch (Exception e) {
      Log.error("Failed to send request to NATS", e);
      throw new RuntimeException("Failed to send request to NATS", e);
    }
  }
  

}