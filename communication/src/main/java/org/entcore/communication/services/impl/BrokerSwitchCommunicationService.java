package org.entcore.communication.services.impl;

import fr.wseduc.webutils.Either;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.entcore.common.migration.AppMigrationConfiguration;
import org.entcore.common.migration.BrokerSwitchConfiguration;
import org.entcore.common.migration.BrokerSwitchType;
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
  private final EventBus eventBus;
  private final AppMigrationConfiguration appMigrationConfiguration;

  /**
   * Constructor for the service switch.
   * At the beginning of the migration of the service, the policy should be set to {@code READ_LEGACY_WRITE_LEGACY} so this system is equivalent to the legacy service.
   * While migrating writes to the new service, the policy should be set to {@code READ_LEGACY_WRITE_BOTH} so that the new service can be used for writing while still reading from the legacy service.
   * After writes have been totally migrated, use the policy {@code READ_NEW_WRITE_BOTH} and progressively add actions to {@code availableReadActionsNewService}.
   * Finally, when all actions have been migrated, use the policy {@code READ_NEW_WRITE_NEW} to only use the new service for both reads and writes.
   *
   * @param delegate The inner communication service to delegate calls to.
   * @param eventBus The Vert.x EventBus for sending messages to the broker.
   */
  public BrokerSwitchCommunicationService(
    final CommunicationService delegate,
    final AppMigrationConfiguration appMigrationConfiguration,
    final EventBus eventBus) {
    this.delegate = delegate;
    this.eventBus = eventBus;
    this.appMigrationConfiguration = appMigrationConfiguration;
  }

  @Override
  public void addLink(String startGroupId, String endGroupId, Handler<Either<String, JsonObject>> handler) {
    if(appMigrationConfiguration.isWriteNew()) {
      sendToBroker("addLink", new JsonObject().put("startGroupId", startGroupId).put("endGroupId", endGroupId), appMigrationConfiguration.isReadNew() ? handler : null);
    }
    if(appMigrationConfiguration.isWriteLegacy()) {
      delegate.addLink(startGroupId, endGroupId, appMigrationConfiguration.isReadLegacy() ? handler : e -> {
      });
    }
  }

  @Override
  public void removeLink(String startGroupId, String endGroupId, Handler<Either<String, JsonObject>> handler) {
    if(appMigrationConfiguration.isWriteNew()) {
      sendToBroker("removeLink", new JsonObject().put("startGroupId", startGroupId).put("endGroupId", endGroupId), (appMigrationConfiguration.isReadNew()) ? handler : null);
    }
    if(appMigrationConfiguration.isWriteLegacy()) {
      delegate.removeLink(startGroupId, endGroupId, (appMigrationConfiguration.isReadLegacy()) ? handler : e -> {
      });
    }
  }

  @Override
  public void addLinkWithUsers(String groupId, Direction direction, Handler<Either<String, JsonObject>> handler) {
    if(appMigrationConfiguration.isWriteNew()) {
      sendToBroker("addLinkWithUsers", new JsonObject().put("groupId", groupId).put("direction", direction.name()), (appMigrationConfiguration.isReadNew()) ? handler : null);
    }
    if(appMigrationConfiguration.isWriteLegacy()) {
      delegate.addLinkWithUsers(groupId, direction, (appMigrationConfiguration.isReadLegacy()) ? handler : e -> {
      });
    }
  }

  @Override
  public void addLinkWithUsers(Map<String, Direction> params, Handler<Either<String, JsonObject>> handler) {
    if(appMigrationConfiguration.isWriteNew()) {
      final JsonObject jsonParams = new JsonObject();
      params.forEach((key, value) -> jsonParams.put(key, value.name()));
      sendToBroker("addLinkWithUsers", new JsonObject().put("params", jsonParams), (appMigrationConfiguration.isReadNew()) ? handler : null);
    }
    if(appMigrationConfiguration.isWriteLegacy()) {
      delegate.addLinkWithUsers(params, (appMigrationConfiguration.isReadLegacy()) ? handler : e -> {
      });
    }
  }

  @Override
  public void removeLinkWithUsers(String groupId, Direction direction, Handler<Either<String, JsonObject>> handler) {
    if(appMigrationConfiguration.isWriteNew()) {
      sendToBroker("removeLinkWithUsers", new JsonObject().put("groupId", groupId).put("direction", direction.name()), (appMigrationConfiguration.isReadNew()) ? handler : null);
    }
    if(appMigrationConfiguration.isWriteLegacy()) {
      delegate.removeLinkWithUsers(groupId, direction, (appMigrationConfiguration.isReadLegacy()) ? handler : e -> {
      });
    }
  }

  @Override
  public void communiqueWith(String groupId, Handler<Either<String, JsonObject>> handler) {
    if(isReadReadyForNewService("communiqueWith")) {
      sendToBroker("communiqueWith", new JsonObject().put("groupId", groupId), handler);
    } else {
      delegate.communiqueWith(groupId, (appMigrationConfiguration.isReadLegacy()) ? handler : e -> {
      });
    }
  }

  @Override
  public void addLinkBetweenRelativeAndStudent(String groupId, Direction direction, Handler<Either<String, JsonObject>> handler) {
    if(appMigrationConfiguration.isWriteNew()) {
      sendToBroker("addLinkBetweenRelativeAndStudent", new JsonObject().put("groupId", groupId).put("direction", direction.name()), (appMigrationConfiguration.isReadNew()) ? handler : null);
    }
    if(appMigrationConfiguration.isWriteLegacy()) {
      delegate.addLinkBetweenRelativeAndStudent(groupId, direction, (appMigrationConfiguration.isReadLegacy()) ? handler : e -> {
      });
    }
  }

  @Override
  public void removeLinkBetweenRelativeAndStudent(String groupId, Direction direction, Handler<Either<String, JsonObject>> handler) {
    if(appMigrationConfiguration.isWriteNew()) {
      sendToBroker("removeLinkBetweenRelativeAndStudent", new JsonObject().put("groupId", groupId).put("direction", direction.name()), (appMigrationConfiguration.isReadNew()) ? handler : null);
    }
    if(appMigrationConfiguration.isWriteLegacy()) {
      delegate.removeLinkBetweenRelativeAndStudent(groupId, direction, (appMigrationConfiguration.isReadLegacy()) ? handler : e -> {
      });
    }
  }

  @Override
  public void initDefaultRules(JsonArray structureIds, JsonObject defaultRules, Integer transactionId, Boolean commit, Handler<Either<String, JsonObject>> handler) {
    if(appMigrationConfiguration.isWriteNew()) {
      sendToBroker("initDefaultRules", new JsonObject()
        .put("structureIds", structureIds)
        .put("defaultRules", defaultRules)
        .put("transactionId", transactionId)
        .put("commit", commit), (appMigrationConfiguration.isReadNew()) ? handler : null);
    }
    if(appMigrationConfiguration.isWriteLegacy()) {
      delegate.initDefaultRules(structureIds, defaultRules, transactionId, commit, (appMigrationConfiguration.isReadLegacy()) ? handler : e -> {
      });
    }
  }

  @Override
  public void initDefaultRules(JsonArray structureIds, JsonObject defaultRules, Handler<Either<String, JsonObject>> handler) {
    if(appMigrationConfiguration.isWriteNew()) {
      sendToBroker("initDefaultRules", new JsonObject()
        .put("structureIds", structureIds)
        .put("defaultRules", defaultRules), (appMigrationConfiguration.isReadNew()) ? handler : null);
    }
    if(appMigrationConfiguration.isWriteLegacy()) {
      delegate.initDefaultRules(structureIds, defaultRules, (appMigrationConfiguration.isReadLegacy()) ? handler : e -> {
      });
    }
  }

  @Override
  public void applyDefaultRules(JsonArray structureIds, Integer transactionId, Boolean commit, Handler<Either<String, JsonObject>> handler) {
    if(appMigrationConfiguration.isWriteNew()) {
      sendToBroker("applyDefaultRules", new JsonObject()
        .put("structureIds", structureIds)
        .put("transactionId", transactionId)
        .put("commit", commit), (appMigrationConfiguration.isReadNew()) ? handler : null);
    }
    if(appMigrationConfiguration.isWriteLegacy()) {
      delegate.applyDefaultRules(structureIds, transactionId, commit, (appMigrationConfiguration.isReadLegacy()) ? handler : e -> {
      });
    }
  }

  @Override
  public void applyDefaultRules(JsonArray structureIds, Handler<Either<String, JsonObject>> handler) {
    if(appMigrationConfiguration.isWriteNew()) {
      sendToBroker("applyDefaultRules", new JsonObject().put("structureIds", structureIds), (appMigrationConfiguration.isReadNew()) ? handler : null);
    }
    if(appMigrationConfiguration.isWriteLegacy()) {
      delegate.applyDefaultRules(structureIds, (appMigrationConfiguration.isReadLegacy()) ? handler : e -> {
      });
    }
  }

  @Override
  public void applyRules(String groupId, Handler<Either<String, JsonObject>> responseHandler) {
    if(appMigrationConfiguration.isWriteNew()) {
      sendToBroker("applyRules", new JsonObject().put("groupId", groupId), (appMigrationConfiguration.isReadNew()) ? responseHandler : null);
    }
    if(appMigrationConfiguration.isWriteLegacy()) {
      delegate.applyRules(groupId, (appMigrationConfiguration.isReadLegacy()) ? responseHandler : e -> {
      });
    }
  }

  @Override
  public void removeRules(String structureId, Handler<Either<String, JsonObject>> handler) {
    if(appMigrationConfiguration.isWriteNew()) {
      sendToBroker("removeRules", new JsonObject().put("structureId", structureId), (appMigrationConfiguration.isReadNew()) ? handler : null);
    }
    if(appMigrationConfiguration.isWriteLegacy()) {
      delegate.removeRules(structureId, (appMigrationConfiguration.isReadLegacy()) ? handler : e -> {
      });
    }
  }


  @Override
  public void visibleUsers(String userId, String structureId, JsonArray expectedTypes, boolean itSelf, boolean myGroup,
                           boolean profile, String preFilter, String customReturn, JsonObject additionalParams,
                           String userProfile, boolean reverseUnion, Handler<Either<String, JsonArray>> handler) {
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
          .put("reverseUnion", reverseUnion)
          .put("userProfile", userProfile)
          .put("additionnalParams", additionalParams),
        handler);
    } else {
      delegate.visibleUsers(userId, structureId, expectedTypes, itSelf, myGroup, profile, preFilter, customReturn, additionalParams, userProfile, reverseUnion, handler);
    }
  }

  @Override
  public void visibleUsers(String userId, String structureId, JsonArray expectedTypes, boolean itSelf, boolean myGroup,
                           boolean profile, String preFilter, String customReturn, JsonObject additionnalParams,
                           Handler<Either<String, JsonArray>> handler) {
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
        .put("additionnalParams", additionnalParams),
        handler);
    } else {
      delegate.visibleUsers(userId, structureId, expectedTypes, itSelf, myGroup, profile, preFilter, customReturn, additionnalParams, handler);
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
    if(appMigrationConfiguration.isWriteNew()) {
      sendToBroker("safelyRemoveLinkWithUsers", new JsonObject().put("groupId", groupId), (appMigrationConfiguration.isReadNew()) ? handler : null);
    }
    if(appMigrationConfiguration.isWriteLegacy()) {
      delegate.safelyRemoveLinkWithUsers(groupId, (appMigrationConfiguration.isReadLegacy()) ? handler : e -> {
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
    if(appMigrationConfiguration.isWriteNew()) {
      sendToBroker("addLinkCheckOnly", new JsonObject()
        .put("startGroupId", startGroupId)
        .put("endGroupId", endGroupId)
        .put("userInfos", JsonObject.mapFrom(userInfos)), (appMigrationConfiguration.isReadNew()) ? handler : null);
    }
    if(appMigrationConfiguration.isWriteLegacy()) {
      delegate.addLinkCheckOnly(startGroupId, endGroupId, userInfos, (appMigrationConfiguration.isReadLegacy()) ? handler : e -> {
      });
    }
  }

  @Override
  public void processChangeDirectionAfterAddingLink(String startGroupId, String endGroupId, Handler<Either<String, JsonObject>> handler) {
    if(appMigrationConfiguration.isWriteNew()) {
      sendToBroker("processChangeDirectionAfterAddingLink", new JsonObject()
        .put("startGroupId", startGroupId)
        .put("endGroupId", endGroupId), (appMigrationConfiguration.isReadNew()) ? handler : null);
    }
    if(appMigrationConfiguration.isWriteLegacy()) {
      delegate.processChangeDirectionAfterAddingLink(startGroupId, endGroupId, (appMigrationConfiguration.isReadLegacy()) ? handler : e -> {
      });
    }
  }

  @Override
  public void removeRelations(String startGroupId, String endGroupId, Handler<Either<String, JsonObject>> handler) {
    if(appMigrationConfiguration.isWriteNew()) {
      sendToBroker("removeRelations", new JsonObject()
        .put("startGroupId", startGroupId)
        .put("endGroupId", endGroupId), (appMigrationConfiguration.isReadNew()) ? handler : null);
    }
    if(appMigrationConfiguration.isWriteLegacy()) {
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
      delegate.verify(senderId, recipientId, (appMigrationConfiguration.isReadLegacy()) ? handler : e -> {
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
    if(appMigrationConfiguration.isWriteNew()) {
      sendToBroker("discoverVisibleAddCommuteUsers", new JsonObject()
        .put("user", JsonObject.mapFrom(user))
        .put("recipientId", recipientId)
        .put("request", getRequestInformation(request)), (appMigrationConfiguration.isReadNew()) ? handler : null);
    }
    if(appMigrationConfiguration.isWriteLegacy()) {
      delegate.discoverVisibleAddCommuteUsers(user, recipientId, request, (appMigrationConfiguration.isReadLegacy()) ? handler : e -> {
      });
    }
  }

  @Override
  public void discoverVisibleRemoveCommuteUsers(String senderId, String recipientId, Handler<Either<String, JsonObject>> handler) {
    if(appMigrationConfiguration.isWriteNew()) {
      sendToBroker("discoverVisibleRemoveCommuteUsers", new JsonObject()
        .put("senderId", senderId)
        .put("recipientId", recipientId), (appMigrationConfiguration.isReadNew()) ? handler : null);
    }
    if(appMigrationConfiguration.isWriteLegacy()) {
      delegate.discoverVisibleRemoveCommuteUsers(senderId, recipientId, (appMigrationConfiguration.isReadLegacy()) ? handler : e -> {
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
    if(appMigrationConfiguration.isWriteNew()) {
      sendToBroker("createDiscoverVisibleGroup", new JsonObject()
        .put("userId", userId)
        .put("body", body), (appMigrationConfiguration.isReadNew()) ? handler : null);
    }
    if(appMigrationConfiguration.isWriteLegacy()) {
      delegate.createDiscoverVisibleGroup(userId, body, (appMigrationConfiguration.isReadLegacy()) ? handler : e -> {
      });
    }
  }

  @Override
  public void updateDiscoverVisibleGroup(String userId, String groupId, JsonObject body, Handler<Either<String, JsonObject>> handler) {
    if(appMigrationConfiguration.isWriteNew()) {
      sendToBroker("updateDiscoverVisibleGroup", new JsonObject()
        .put("userId", userId)
        .put("groupId", groupId)
        .put("body", body), (appMigrationConfiguration.isReadNew()) ? handler : null);
    }
    if(appMigrationConfiguration.isWriteLegacy()) {
      delegate.updateDiscoverVisibleGroup(userId, groupId, body, (appMigrationConfiguration.isReadLegacy()) ? handler : e -> {
      });
    }
  }

  @Override
  public void addDiscoverVisibleGroupUsers(UserInfos user, String groupId, JsonObject body, HttpServerRequest request, Handler<Either<String, JsonObject>> handler) {
    if(appMigrationConfiguration.isWriteNew()) {
      sendToBroker("addDiscoverVisibleGroupUsers", new JsonObject()
        .put("user", JsonObject.mapFrom(user))
        .put("groupId", groupId)
        .put("body", body)
        .put("request", getRequestInformation(request)), (appMigrationConfiguration.isReadNew()) ? handler : null);
    }
    if(appMigrationConfiguration.isWriteLegacy()) {
      delegate.addDiscoverVisibleGroupUsers(user, groupId, body, request, (appMigrationConfiguration.isReadLegacy()) ? handler : e -> {
      });
    }
  }

  @Override
  public void getDiscoverVisibleAcceptedProfile(Handler<Either<String, JsonArray>> handler) {
    delegate.getDiscoverVisibleAcceptedProfile(handler);
  }

  @Override
  public Future<JsonArray> searchVisibles(UserInfos user, String search, String language) {
    final Future<JsonArray> future;
    if(appMigrationConfiguration.isReadEnabled("searchVisibles")) {
      final Promise<JsonArray> promise = Promise.promise();
      sendToBroker("searchVisibles", new JsonObject()
        .put("user", JsonObject.mapFrom(user))
        .put("search", search)
        .put("language", language), promise);
      future = promise.future();
    } else {
      future = delegate.searchVisibles(user, search, language);
    }
    return future;
  }

  private <T> void sendToBroker(final String action, final JsonObject params, final Handler handler) {
    final JsonObject payload = new JsonObject()
      .put("action", action)
      .put("service", "referential")
      .put("params", params);
    final String address = BrokerSwitchConfiguration.LEGACY_MIGRATION_ADDRESS;
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
    return (appMigrationConfiguration.isReadNew()) && appMigrationConfiguration.getAvailableReadActions().contains(action);
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
