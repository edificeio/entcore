package org.entcore.common.audience;

import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.entcore.common.user.RepositoryEvents;

import java.util.List;
import java.util.Optional;

/**
 * 
 */
public class AudienceRepositoryEvents implements RepositoryEvents {
  private static final Logger log = LoggerFactory.getLogger(AudienceRepositoryEvents.class);

  /**
   * Proxyfied events repository that will be used to import/export resources.
   */
  private final RepositoryEvents realRepositoryEvents;

  public AudienceRepositoryEvents(RepositoryEvents realRepositoryEvents) {
    this.realRepositoryEvents = realRepositoryEvents;
  }

  @Override
  public void deleteUsers(JsonArray users, Handler<List<ResourceChanges>> handler) {
    // TODO implement
    throw new UnsupportedOperationException("implement.deleteUsers");
  }

  @Override
  public void mergeUsers(String keepedUserId, String deletedUserId) {
    // TODO implement
    throw new UnsupportedOperationException("implement.mergeUsers");
  }

  @Override
  public void removeShareGroups(JsonArray oldGroups) {
    // TODO implement
    throw new UnsupportedOperationException("implement.removeShareGroups");
  }

  @Override
  public void exportResources(boolean exportDocuments, boolean exportSharedResources, String exportId, String userId, JsonArray groups, String exportPath,
                              String locale, String host, Handler<Boolean> handler) {
    realRepositoryEvents.exportResources(exportDocuments, exportSharedResources, exportId, userId, groups, exportPath, locale, host, handler);
  }

  @Override
  public void exportResources(JsonArray resourcesIds, boolean exportDocuments, boolean exportSharedResources, String exportId, String userId,
                              JsonArray groups, String exportPath, String locale, String host, Handler<Boolean> handler) {
    realRepositoryEvents.exportResources(resourcesIds, exportDocuments, exportSharedResources, exportId, userId, groups, exportPath, locale, host, handler);
  }

  @Override
  public void importResources(String importId, String userId, String userLogin, String userName, String importPath,
                              String locale, String host, boolean forceImportAsDuplication, Handler<JsonObject> handler) {
    realRepositoryEvents.importResources(importId, userId, userLogin, userName, importPath, locale, host, forceImportAsDuplication, handler);
  }

  @Override
  public void deleteGroups(JsonArray groups, Handler<List<ResourceChanges>> handler) {
    realRepositoryEvents.deleteGroups(groups, handler);
  }

  @Override
  public void usersClassesUpdated(JsonArray updates) {
    realRepositoryEvents.usersClassesUpdated(updates);
  }

  @Override
  public void transition(JsonObject structure) {
    realRepositoryEvents.transition(structure);
  }

  @Override
  public void tenantsStructuresUpdated(JsonArray addedTenantsStructures, JsonArray deletedTenantsStructures) {
    realRepositoryEvents.tenantsStructuresUpdated(addedTenantsStructures, deletedTenantsStructures);
  }

  @Override
  public void timetableImported(String uai) {
    realRepositoryEvents.timetableImported(uai);
  }

  @Override
  public Optional<String> getMainRepositoryName() {
    return realRepositoryEvents.getMainRepositoryName();
  }
}
