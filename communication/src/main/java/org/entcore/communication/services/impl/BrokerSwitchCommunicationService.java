package org.entcore.communication.services.impl;

import fr.wseduc.webutils.Either;
import io.vertx.core.Handler;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.entcore.common.user.UserInfos;
import org.entcore.communication.services.CommunicationService;

import java.util.Map;
import java.util.Set;

/**
 * This service acts as a switch between the new and legacy communication services.
 * Depending on the broker switch type, it delegates calls to either the new or legacy service.
 * It allows for a gradual migration from the legacy service to the new one without breaking existing functionality by
 * selectively toggling writes and reads for new and legacy implementations.
 */
public class BrokerSwitchCommunicationService implements CommunicationService {
  private final CommunicationService delegate;
  private final boolean writeNew;
  private final boolean writeLegacy;
  private final boolean readNew;
  private final boolean readLegacy;
  private final EventBus eventBus;
  private final Set<String> availableReadActionsNewService;

  /**
   * Constructor for the service switch.
   * At the beginning of the migration of the service, the policy should be set to {@code READ_LEGACY_WRITE_LEGACY} so this system is equivalent to the legacy service.
   * While migrating writes to the new service, the policy should be set to {@code READ_LEGACY_WRITE_BOTH} so that the new service can be used for writing while still reading from the legacy service.
   * After writes have been totally migrated, use the policy {@code READ_NEW_WRITE_BOTH} and progressively add actions to {@code availableReadActionsNewService}.
   * Finally, when all actions have been migrated, use the policy {@code READ_NEW_WRITE_NEW} to only use the new service for both reads and writes.
   *
   * @param delegate The inner communication service to delegate calls to.
   * @param brokerSwitchType The type of broker switch to determine read/write behavior. Dependiong on the supplied value, it will :<ul>
   *                         <li>READ_LEGACY_WRITE_BOTH: Read from legacy, write to both new and legacy.</li>
   *                         <li>READ_LEGACY_WRITE_LEGACY: Read from legacy, write to legacy only.</li>
   *                         <li>READ_NEW_WRITE_BOTH: Read from new service iff the action is in {@code availableReadActionsNewService} otherwise read from legacy, write to both new and legacy.</li>
   *                         <li>READ_NEW_WRITE_NEW: Read from new service and never from legacy, write to new service only.</li>
   * </ul>
   *
   * @param availableReadActionsNewService Set of read only actions for which the new service is available.
   * @param eventBus The Vert.x EventBus for sending messages to the broker.
   */
  public BrokerSwitchCommunicationService(
    final CommunicationService delegate,
    final BrokerSwitchType brokerSwitchType,
    final Set<String> availableReadActionsNewService,
    final EventBus eventBus) {
    this.delegate = delegate;
    this.eventBus = eventBus;
    this.availableReadActionsNewService = availableReadActionsNewService;
    switch (brokerSwitchType) {
      case READ_LEGACY_WRITE_BOTH:
        this.writeNew = true;
        this.writeLegacy = true;
        this.readNew = false;
        this.readLegacy = true;
        break;
      case READ_LEGACY_WRITE_LEGACY:
        this.writeNew = false;
        this.writeLegacy = true;
        this.readNew = false;
        this.readLegacy = true;
        break;
      case READ_NEW_WRITE_BOTH:
        this.writeNew = true;
        this.writeLegacy = true;
        this.readNew = true;
        this.readLegacy = false;
        break;
      case READ_NEW_WRITE_NEW:
        this.writeNew = true;
        this.writeLegacy = false;
        this.readNew = true;
        this.readLegacy = false;
        break;
      default:
        throw new IllegalArgumentException("Invalid BrokerSwitchType: " + brokerSwitchType);
    }
  }

  @Override
  public void addLink(String startGroupId, String endGroupId, Handler<Either<String, JsonObject>> handler) {
    if(writeNew) {
      sendToBroker("addLink", new JsonObject().put("startGroupId", startGroupId).put("endGroupId", endGroupId), readNew ? handler : null);
    }
    if(writeLegacy) {
      delegate.addLink(startGroupId, endGroupId, readLegacy ? handler : e -> {
      });
    }
  }

  @Override
  public void removeLink(String startGroupId, String endGroupId, Handler<Either<String, JsonObject>> handler) {
    if(writeNew) {
      sendToBroker("removeLink", new JsonObject().put("startGroupId", startGroupId).put("endGroupId", endGroupId), readNew ? handler : null);
    }
    if(writeLegacy) {
      delegate.removeLink(startGroupId, endGroupId, readLegacy ? handler : e -> {
      });
    }
  }

  @Override
  public void addLinkWithUsers(String groupId, Direction direction, Handler<Either<String, JsonObject>> handler) {
    if(writeNew) {
      sendToBroker("addLinkWithUsers", new JsonObject().put("groupId", groupId).put("direction", direction.name()), readNew ? handler : null);
    }
    if(writeLegacy) {
      delegate.addLinkWithUsers(groupId, direction, readLegacy ? handler : e -> {
      });
    }
  }

  @Override
  public void addLinkWithUsers(Map<String, Direction> params, Handler<Either<String, JsonObject>> handler) {
    if(writeNew) {
      final JsonObject jsonParams = new JsonObject();
      params.forEach((key, value) -> jsonParams.put(key, value.name()));
      sendToBroker("addLinkWithUsers", new JsonObject().put("params", jsonParams), readNew ? handler : null);
    }
    if(writeLegacy) {
      delegate.addLinkWithUsers(params, readLegacy ? handler : e -> {
      });
    }
  }

  @Override
  public void removeLinkWithUsers(String groupId, Direction direction, Handler<Either<String, JsonObject>> handler) {
    if(writeNew) {
      sendToBroker("removeLinkWithUsers", new JsonObject().put("groupId", groupId).put("direction", direction.name()), readNew ? handler : null);
    }
    if(writeLegacy) {
      delegate.removeLinkWithUsers(groupId, direction, readLegacy ? handler : e -> {
      });
    }
  }

  @Override
  public void communiqueWith(String groupId, Handler<Either<String, JsonObject>> handler) {
    if(isReadReadyForNewService("communiqueWith")) {
      sendToBroker("communiqueWith", new JsonObject().put("groupId", groupId), handler);
    } else {
      delegate.communiqueWith(groupId, readLegacy ? handler : e -> {
      });
    }
  }

  @Override
  public void addLinkBetweenRelativeAndStudent(String groupId, Direction direction, Handler<Either<String, JsonObject>> handler) {
    if(writeNew) {
      sendToBroker("addLinkBetweenRelativeAndStudent", new JsonObject().put("groupId", groupId).put("direction", direction.name()), readNew ? handler : null);
    }
    if(writeLegacy) {
      delegate.addLinkBetweenRelativeAndStudent(groupId, direction, readLegacy ? handler : e -> {
      });
    }
  }

  @Override
  public void removeLinkBetweenRelativeAndStudent(String groupId, Direction direction, Handler<Either<String, JsonObject>> handler) {
    if(writeNew) {
      sendToBroker("removeLinkBetweenRelativeAndStudent", new JsonObject().put("groupId", groupId).put("direction", direction.name()), readNew ? handler : null);
    }
    if(writeLegacy) {
      delegate.removeLinkBetweenRelativeAndStudent(groupId, direction, readLegacy ? handler : e -> {
      });
    }
  }

  @Override
  public void initDefaultRules(JsonArray structureIds, JsonObject defaultRules, Integer transactionId, Boolean commit, Handler<Either<String, JsonObject>> handler) {
    if(writeNew) {
      sendToBroker("initDefaultRules", new JsonObject()
        .put("structureIds", structureIds)
        .put("defaultRules", defaultRules)
        .put("transactionId", transactionId)
        .put("commit", commit), readNew ? handler : null);
    }
    if(writeLegacy) {
      delegate.initDefaultRules(structureIds, defaultRules, transactionId, commit, readLegacy ? handler : e -> {
      });
    }
  }

  @Override
  public void initDefaultRules(JsonArray structureIds, JsonObject defaultRules, Handler<Either<String, JsonObject>> handler) {
    if(writeNew) {
      sendToBroker("initDefaultRules", new JsonObject()
        .put("structureIds", structureIds)
        .put("defaultRules", defaultRules), readNew ? handler : null);
    }
    if(writeLegacy) {
      delegate.initDefaultRules(structureIds, defaultRules, readLegacy ? handler : e -> {
      });
    }
  }

  @Override
  public void applyDefaultRules(JsonArray structureIds, Integer transactionId, Boolean commit, Handler<Either<String, JsonObject>> handler) {
    if(writeNew) {
      sendToBroker("applyDefaultRules", new JsonObject()
        .put("structureIds", structureIds)
        .put("transactionId", transactionId)
        .put("commit", commit), readNew ? handler : null);
    }
    if(writeLegacy) {
      delegate.applyDefaultRules(structureIds, transactionId, commit, readLegacy ? handler : e -> {
      });
    }
  }

  @Override
  public void applyDefaultRules(JsonArray structureIds, Handler<Either<String, JsonObject>> handler) {
    if(writeNew) {
      sendToBroker("applyDefaultRules", new JsonObject().put("structureIds", structureIds), readNew ? handler : null);
    }
    if(writeLegacy) {
      delegate.applyDefaultRules(structureIds, readLegacy ? handler : e -> {
      });
    }
  }

  @Override
  public void applyRules(String groupId, Handler<Either<String, JsonObject>> responseHandler) {
    if(writeNew) {
      sendToBroker("applyRules", new JsonObject().put("groupId", groupId), readNew ? responseHandler : null);
    }
    if(writeLegacy) {
      delegate.applyRules(groupId, readLegacy ? responseHandler : e -> {
      });
    }
  }

  @Override
  public void removeRules(String structureId, Handler<Either<String, JsonObject>> handler) {
    if(writeNew) {
      sendToBroker("removeRules", new JsonObject().put("structureId", structureId), readNew ? handler : null);
    }
    if(writeLegacy) {
      delegate.removeRules(structureId, readLegacy ? handler : e -> {
      });
    }
  }

  @Override
  public void visibleUsers(String userId, String structureId, JsonArray expectedTypes,
                           boolean itSelf, boolean myGroup, boolean profile,
                           String preFilter, String customReturn, JsonObject additionnalParams, Handler<Either<String, JsonArray>> handler) {
    if(isReadReadyForNewService("visibleUsers")) {
      sendToBroker("visibleUsers", new JsonObject()
        .put("userId", userId)
        .put("structureId", structureId)
        .put("expectedTypes", expectedTypes)
        .put("itSelf", itSelf)
        .put("myGroup", myGroup)
        .put("profile", profile)
        .put("preFilter", preFilter)
        .put("customReturn", customReturn)
        .put("additionnalParams", additionnalParams), handler);
    } else {
      delegate.visibleUsers(userId, structureId, expectedTypes, itSelf, myGroup, profile, preFilter, customReturn, additionnalParams, handler);
    }
  }

  @Override
  public void visibleUsers(String userId, String structureId, JsonArray expectedTypes, boolean itSelf, boolean myGroup,
                           boolean profile, String preFilter, String customReturn, JsonObject additionnalParams,
                           String userProfile, Handler<Either<String, JsonArray>> handler) {
    if(isReadReadyForNewService("visibleUsers")) {
      sendToBroker("visibleUsers", new JsonObject()
        .put("userId", userId)
        .put("structureId", structureId)
        .put("expectedTypes", expectedTypes)
        .put("itSelf", itSelf)
        .put("myGroup", myGroup)
        .put("profile", profile)
        .put("preFilter", preFilter)
        .put("customReturn", customReturn)
        .put("additionnalParams", additionnalParams)
        .put("userProfile", userProfile), handler);
    } else {
      delegate.visibleUsers(userId, structureId, expectedTypes, itSelf, myGroup, profile, preFilter, customReturn, additionnalParams, userProfile, handler);
    }
  }

  @Override
  public void usersCanSeeMe(String userId, Handler<Either<String, JsonArray>> handler) {
    if(isReadReadyForNewService("usersCanSeeMe")) {
      sendToBroker("usersCanSeeMe", new JsonObject().put("userId", userId), handler);
    } else {
      delegate.usersCanSeeMe(userId, handler);
    }
  }

  @Override
  public void visibleProfilsGroups(String userId, String customReturn, JsonObject additionnalParams, String preFilter, Handler<Either<String, JsonArray>> handler) {
    if(isReadReadyForNewService("visibleProfilsGroups")) {
      sendToBroker("visibleProfilsGroups", new JsonObject()
        .put("userId", userId)
        .put("customReturn", customReturn)
        .put("additionnalParams", additionnalParams)
        .put("preFilter", preFilter), handler);
    } else {
      delegate.visibleProfilsGroups(userId, customReturn, additionnalParams, preFilter, handler);
    }
  }

  @Override
  public void visibleManualGroups(String userId, String customReturn, JsonObject additionnalParams, Handler<Either<String, JsonArray>> handler) {
    if(isReadReadyForNewService("visibleManualGroups")) {
      sendToBroker("visibleManualGroups", new JsonObject()
        .put("userId", userId)
        .put("customReturn", customReturn)
        .put("additionnalParams", additionnalParams), handler);
    } else {
      delegate.visibleManualGroups(userId, customReturn, additionnalParams, handler);
    }
  }

  @Override
  public void getOutgoingRelations(String id, Handler<Either<String, JsonArray>> results) {
    if(isReadReadyForNewService("getOutgoingRelations")) {
      sendToBroker("getOutgoingRelations", new JsonObject().put("id", id), results);
    } else {
      delegate.getOutgoingRelations(id, results);
    }
  }

  @Override
  public void getIncomingRelations(String id, Handler<Either<String, JsonArray>> results) {
    if(isReadReadyForNewService("getIncomingRelations")) {
      sendToBroker("getIncomingRelations", new JsonObject().put("id", id), results);
    } else {
      delegate.getIncomingRelations(id, results);
    }
  }

  @Override
  public void safelyRemoveLinkWithUsers(String groupId, Handler<Either<String, JsonObject>> handler) {
    if(writeNew) {
      sendToBroker("safelyRemoveLinkWithUsers", new JsonObject().put("groupId", groupId), readNew ? handler : null);
    }
    if(writeLegacy) {
      delegate.safelyRemoveLinkWithUsers(groupId, readLegacy ? handler : e -> {
      });
    }
  }

  @Override
  public void getDirections(String startGroupId, String endGroupId, Handler<Either<String, JsonObject>> handler) {
    if(isReadReadyForNewService("getDirections")) {
      sendToBroker("getDirections", new JsonObject().put("startGroupId", startGroupId).put("endGroupId", endGroupId), handler);
    } else {
      delegate.getDirections(startGroupId, endGroupId, handler);
    }
  }

  @Override
  public void addLinkCheckOnly(String startGroupId, String endGroupId, UserInfos userInfos, Handler<Either<String, JsonObject>> handler) {
    if(writeNew) {
      sendToBroker("addLinkCheckOnly", new JsonObject()
        .put("startGroupId", startGroupId)
        .put("endGroupId", endGroupId)
        .put("userInfos", JsonObject.mapFrom(userInfos)), readNew ? handler : null);
    } else if(writeLegacy) {
      delegate.addLinkCheckOnly(startGroupId, endGroupId, userInfos, readLegacy ? handler : e -> {
      });
    }
  }

  @Override
  public void processChangeDirectionAfterAddingLink(String startGroupId, String endGroupId, Handler<Either<String, JsonObject>> handler) {
    if(writeNew) {
      sendToBroker("processChangeDirectionAfterAddingLink", new JsonObject()
        .put("startGroupId", startGroupId)
        .put("endGroupId", endGroupId), readNew ? handler : null);
    } else if(writeLegacy) {
      delegate.processChangeDirectionAfterAddingLink(startGroupId, endGroupId, readLegacy ? handler : e -> {
      });
    }
  }

  @Override
  public void removeRelations(String startGroupId, String endGroupId, Handler<Either<String, JsonObject>> handler) {
    if(writeNew) {
      sendToBroker("removeRelations", new JsonObject()
        .put("startGroupId", startGroupId)
        .put("endGroupId", endGroupId), readNew ? handler : null);
    }
    if(writeLegacy) {
      delegate.removeRelations(startGroupId, endGroupId, handler);
    }
  }

  @Override
  public void verify(String senderId, String recipientId, Handler<Either<String, JsonObject>> handler) {
    if(isReadReadyForNewService("verify")) {
      sendToBroker("verify", new JsonObject()
        .put("senderId", senderId)
        .put("recipientId", recipientId), handler);
    } else {
      delegate.verify(senderId, recipientId, readLegacy ? handler : e -> {
      });
    }
  }

  @Override
  public void getDiscoverVisibleUsers(String userId, JsonObject filter, Handler<Either<String, JsonArray>> handler) {
    if(isReadReadyForNewService("getDiscoverVisibleUsers")) {
      sendToBroker("getDiscoverVisibleUsers", new JsonObject()
        .put("userId", userId)
        .put("filter", filter), handler);
    } else {
      delegate.getDiscoverVisibleUsers(userId, filter, handler);
    }
  }

  @Override
  public void getDiscoverVisibleStructures(Handler<Either<String, JsonArray>> handler) {
    if(isReadReadyForNewService("getDiscoverVisibleStructures")) {
      sendToBroker("getDiscoverVisibleStructures", new JsonObject(), handler);
    } else {
      delegate.getDiscoverVisibleStructures(handler);
    }
  }

  @Override
  public void discoverVisibleAddCommuteUsers(UserInfos user, String recipientId, HttpServerRequest request, Handler<Either<String, JsonObject>> handler) {
    if(writeNew) {
      sendToBroker("discoverVisibleAddCommuteUsers", new JsonObject()
        .put("user", JsonObject.mapFrom(user))
        .put("recipientId", recipientId)
        .put("request", getRequestInformation(request)), readNew ? handler : null);
    }
    if(writeLegacy) {
      delegate.discoverVisibleAddCommuteUsers(user, recipientId, request, readLegacy ? handler : e -> {
      });
    }
  }

  @Override
  public void discoverVisibleRemoveCommuteUsers(String senderId, String recipientId, Handler<Either<String, JsonObject>> handler) {
    if(writeNew) {
      sendToBroker("discoverVisibleRemoveCommuteUsers", new JsonObject()
        .put("senderId", senderId)
        .put("recipientId", recipientId), readNew ? handler : null);
    }
    if(writeLegacy) {
      delegate.discoverVisibleRemoveCommuteUsers(senderId, recipientId, readLegacy ? handler : e -> {
      });
    }
  }

  @Override
  public void discoverVisibleGetGroups(String userId, Handler<Either<String, JsonArray>> handler) {
    if(isReadReadyForNewService("discoverVisibleGetGroups")) {
      sendToBroker("discoverVisibleGetGroups", new JsonObject().put("userId", userId), handler);
    } else {
      delegate.discoverVisibleGetGroups(userId, handler);
    }
  }

  @Override
  public void discoverVisibleGetUsersInGroup(String userId, String groupId, Handler<Either<String, JsonArray>> handler) {
    if(isReadReadyForNewService("discoverVisibleGetUsersInGroup")) {
      sendToBroker("discoverVisibleGetUsersInGroup", new JsonObject()
        .put("userId", userId)
        .put("groupId", groupId), handler);
    } else {
      delegate.discoverVisibleGetUsersInGroup(userId, groupId, handler);
    }
  }

  @Override
  public void createDiscoverVisibleGroup(String userId, JsonObject body, Handler<Either<String, JsonObject>> handler) {
    if(writeNew) {
      sendToBroker("createDiscoverVisibleGroup", new JsonObject()
        .put("userId", userId)
        .put("body", body), readNew ? handler : null);
    }
    if(writeLegacy) {
      delegate.createDiscoverVisibleGroup(userId, body, readLegacy ? handler : e -> {
      });
    }
  }

  @Override
  public void updateDiscoverVisibleGroup(String userId, String groupId, JsonObject body, Handler<Either<String, JsonObject>> handler) {
    if(writeNew) {
      sendToBroker("updateDiscoverVisibleGroup", new JsonObject()
        .put("userId", userId)
        .put("groupId", groupId)
        .put("body", body), readNew ? handler : null);
    }
    if(writeLegacy) {
      delegate.updateDiscoverVisibleGroup(userId, groupId, body, readLegacy ? handler : e -> {
      });
    }
  }

  @Override
  public void addDiscoverVisibleGroupUsers(UserInfos user, String groupId, JsonObject body, HttpServerRequest request, Handler<Either<String, JsonObject>> handler) {
    if(writeNew) {
      sendToBroker("addDiscoverVisibleGroupUsers", new JsonObject()
        .put("user", JsonObject.mapFrom(user))
        .put("groupId", groupId)
        .put("body", body)
        .put("request", getRequestInformation(request)), readNew ? handler : null);
    }
    if(writeLegacy) {
      delegate.addDiscoverVisibleGroupUsers(user, groupId, body, request, readLegacy ? handler : e -> {
      });
    }
  }

  @Override
  public void getDiscoverVisibleAcceptedProfile(Handler<Either<String, JsonArray>> handler) {
    delegate.getDiscoverVisibleAcceptedProfile(handler);
  }

  private <T> void sendToBroker(final String action, final JsonObject params, final Handler handler) {
    final JsonObject payload = new JsonObject()
      .put("action", action)
      .put("service", "communication")
      .put("params", params);
    final String address = "broker.proxy.legacy.migration";
    if(handler == null) {
      eventBus.send(address, payload);
    } else {
      eventBus.request(address, payload, reply -> {
        if (reply.succeeded()) {
          final Either<String, T> result = new Either.Right<>((T) reply.result().body());
          handler.handle(result);
        } else {
          final Either<String, T> error = new Either.Left<>(reply.cause().getMessage());
          handler.handle(error);
        }
      });
    }
  }

  private boolean isReadReadyForNewService(final String action) {
    return readNew && availableReadActionsNewService.contains(action);
  }

  private JsonObject getRequestInformation(final HttpServerRequest request) {
    final JsonObject httpInfo = new JsonObject();
    if (request != null) {
      httpInfo.put("method", request.method().name())
              .put("uri", request.uri())
              .put("headers", request.headers())
              .put("remoteAddress", request.remoteAddress().toString())
              .put("localAddress", request.localAddress().toString())
              .put("ssl", request.isSSL());
    }
    return httpInfo;
  }
}
