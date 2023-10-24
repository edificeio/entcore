package org.entcore.workspace.service.impl;

import com.mongodb.client.model.Filters;
import fr.wseduc.mongodb.MongoDb;
import fr.wseduc.mongodb.MongoQueryBuilder;
import fr.wseduc.webutils.security.SecuredAction;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.json.JsonObject;
import org.bson.conversions.Bson;
import org.entcore.common.share.impl.MongoDbShareService;
import org.entcore.workspace.dao.DocumentDao;

import java.util.List;
import java.util.Map;

import static fr.wseduc.webutils.Utils.isEmpty;

public class WorkspaceShareService extends MongoDbShareService {

  public WorkspaceShareService(EventBus eb, MongoDb mongo, String collection, Map<String, SecuredAction> securedActions, Map<String, List<String>> groupedActions) {
    super(eb, mongo, collection, securedActions, groupedActions);
  }
  @Override
  public Future<String> getResourceOwnerUserId(String resourceId) {
    final Bson query = Filters.eq("_id", resourceId);
    final JsonObject keys = new JsonObject().put("owner", 1);
    final Promise<String> promise = Promise.promise();
    mongo.findOne(DocumentDao.DOCUMENTS_COLLECTION, MongoQueryBuilder.build(query), keys, mongoEvent -> {
      if ("ok".equals(mongoEvent.body().getString("status"))) {
        final JsonObject body = mongoEvent.body().getJsonObject("result");
        final String ownerId = body.getString("owner");
        if(body == null || isEmpty(ownerId)) {
          promise.fail("No owner id found for " + resourceId);
        } else {
          promise.complete(ownerId);
        }
      } else {
        promise.fail("Error getting owner id of " + resourceId + " : " + mongoEvent.body().getString("message"));
      }
    });
    return promise.future();
  }
}
