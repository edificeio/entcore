package org.entcore.directory.services.impl;

import fr.wseduc.webutils.Either;
import io.vertx.core.Handler;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.entcore.common.migration.AppMigrationConfiguration;
import org.entcore.common.user.UserInfos;
import org.entcore.directory.services.ClassService;

public class BrokerSwitchClassService implements ClassService {
  private static final Logger log = LoggerFactory.getLogger(BrokerSwitchClassService.class);
  private final EventBus eventBus;
  private final ClassService delegate;
  private final AppMigrationConfiguration appMigrationConfiguration;

  public BrokerSwitchClassService(EventBus eventBus, ClassService delegate, AppMigrationConfiguration appMigrationConfiguration) {
    this.eventBus = eventBus;
    this.delegate = delegate;
    this.appMigrationConfiguration = appMigrationConfiguration;
  }

  @Override
  public void create(String schoolId, JsonObject c, Handler<Either<String, JsonObject>> result) {
    // Passing through to the delegate because the underlying implementation calls feeder
    delegate.create(schoolId, c, result);
  }

  @Override
  public void update(String classId, JsonObject c, Handler<Either<String, JsonObject>> result) {
    // Passing through to the delegate because the underlying implementation calls feeder
    delegate.update(classId, c, result);
  }

  @Override
  public void remove(String classId, Handler<Either<String, JsonObject>> result) {
    // Passing through to the delegate because the underlying implementation calls feeder
    delegate.remove(classId, result);
  }

  @Override
  public void findUsers(String classId, JsonArray expectedTypes, boolean collectRelative, Handler<Either<String, JsonArray>> results) {
    if (appMigrationConfiguration.isReadEnabled("class.findUsers")) {
      results.handle(new Either.Left<>("class.findUsers is not yet implemented"));
    } else {
      delegate.findUsers(classId, expectedTypes, collectRelative, results);
    }
  }

  @Override
  public void get(String classId, Handler<Either<String, JsonObject>> result) {
    if (appMigrationConfiguration.isReadEnabled("class.findUsers")) {
      result.handle(new Either.Left<>("class.get is not yet implemented"));
    } else {
      delegate.get(classId, result);
    }
  }

  @Override
  public void addSelf(String classId, UserInfos user, Handler<Either<String, JsonObject>> result) {
    if(appMigrationConfiguration.isWriteNew()) {
      result.handle(new Either.Left<>("addSelf is not implemented in the new service"));
    } else {
      delegate.addSelf(classId, user, result);
    }
  }

  @Override
  public void addUser(String classId, String userId, UserInfos user, Handler<Either<String, JsonObject>> result) {
    if(appMigrationConfiguration.isWriteNew()) {
      result.handle(new Either.Left<>("addUser is not implemented in the new service"));
    } else {
      delegate.addUser(classId, userId, user, result);
    }
  }

  @Override
  public void link(String classId, JsonArray userIds, Handler<Either<String, JsonArray>> result) {
    // Passing through to the delegate because the underlying implementation calls feeder
    delegate.link(classId, userIds, result);
  }

  @Override
  public void link(String classId, String userId, Handler<Either<String, JsonObject>> result) {
    // Passing through to the delegate because the underlying implementation calls feeder
    delegate.link(classId, userId, result);
  }

  @Override
  public void unlink(String classId, String userId, Handler<Either<String, JsonObject>> result) {
    // Passing through to the delegate because the underlying implementation calls feeder
    delegate.unlink(classId, userId, result);
  }

  @Override
  public void unlink(JsonArray classIds, JsonArray userIds, Handler<Either<String, JsonArray>> result) {
    // Passing through to the delegate because the underlying implementation calls feeder
    delegate.unlink(classIds, userIds, result);
  }

  @Override
  public void listAdmin(String structureId, UserInfos userInfos, Handler<Either<String, JsonArray>> results) {
    if(appMigrationConfiguration.isReadEnabled("class.listAdmin")) {
      results.handle(new Either.Left<>("class.listAdmin is not yet implemented"));
    } else {
      delegate.listAdmin(structureId, userInfos, results);
    }
  }

  @Override
  public void listDetachedUsers(JsonArray structureIds, UserInfos user, Handler<JsonArray> handler) {
    if(appMigrationConfiguration.isReadEnabled("class.listDetachedUsers")) {
      log.warn("Called unimplemented method listDetachedUsers in BrokerSwitchClassService");
      handler.handle(null);
    } else {
      delegate.listDetachedUsers(structureIds, user, handler);
    }
  }

  @Override
  public void findVisibles(UserInfos user, String classId, boolean collectRelative, Handler<JsonArray> handler) {
    if (appMigrationConfiguration.isReadEnabled("class.findVisibles")) {
      log.warn("Called unimplemented method findVisibles in BrokerSwitchClassService");
      handler.handle(null);
    } else {
      delegate.findVisibles(user, classId, collectRelative, handler);
    }
  }
}
