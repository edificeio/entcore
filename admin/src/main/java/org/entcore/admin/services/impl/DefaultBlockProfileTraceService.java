package org.entcore.admin.services.impl;

import com.mongodb.client.model.Filters;
import fr.wseduc.mongodb.MongoDb;
import fr.wseduc.mongodb.MongoQueryBuilder;
import fr.wseduc.webutils.Either;
import fr.wseduc.webutils.request.filter.Filter;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.bson.conversions.Bson;
import org.entcore.admin.services.BlockProfileTraceService;
import org.entcore.common.service.impl.MongoDbCrudService;

import java.util.ArrayList;
import java.util.List;

import static org.entcore.common.mongodb.MongoDbResult.*;

public class DefaultBlockProfileTraceService extends MongoDbCrudService implements BlockProfileTraceService {

    public DefaultBlockProfileTraceService(String collection) {
        super(collection);
    }

    @Override
    public void createTrace(JsonObject data, Handler<Either<String, JsonObject>> handler) {
        JsonObject queryData = new JsonObject()
                .put("action", data.getString("action"))
                .put("profile", data.getString("profile"))
                .put("structureId", data.getString("structureId"));

        JsonObject now = MongoDb.now();
        queryData.put("created", now).put("modified", now);

        MongoDbCrudService.setUserMetadata(queryData, data.getString("userId"), data.getString("userName"));

        mongo.save(this.collection, queryData, validResultHandler(handler));
    }

    @Override
    public void listByStructureId(String structureId, Handler<Either<String, JsonArray>> handler) {
        List<String> actions = new ArrayList<>();
        actions.add("BLOCK");
        actions.add("UNBLOCK");
        // Query
        Bson query = Filters.and(
                Filters.eq("structureId", structureId),
                Filters.in("action", actions)
                );
        mongo.find(this.collection, MongoQueryBuilder.build(query), validResultsHandler(handler));
    }
}
