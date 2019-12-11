package org.entcore.admin.services.impl;

import com.mongodb.QueryBuilder;
import fr.wseduc.mongodb.MongoQueryBuilder;
import fr.wseduc.webutils.Either;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.entcore.admin.services.BlockProfileTraceService;
import org.entcore.common.service.impl.MongoDbCrudService;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static org.entcore.common.mongodb.MongoDbResult.validResultsHandler;

public class DefaultBlockProfileTraceService extends MongoDbCrudService implements BlockProfileTraceService {

    public DefaultBlockProfileTraceService(String collection) {
        super(collection);
    }

    @Override
    public void listByStructureId(String structureId, Handler<Either<String, JsonArray>> handler) {
        List<String> actions = new ArrayList<>();
        actions.add("BLOCK");
        actions.add("UNBLOCK");
        // Query
        QueryBuilder query = QueryBuilder
                .start("structureId").is(structureId)
                .and(QueryBuilder.start("action").in(actions).get());
        mongo.find(this.collection, MongoQueryBuilder.build(query), validResultsHandler(handler));
    }
}
