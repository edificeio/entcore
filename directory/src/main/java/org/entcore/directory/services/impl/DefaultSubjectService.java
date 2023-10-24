package org.entcore.directory.services.impl;

import fr.wseduc.webutils.Either;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.entcore.common.neo4j.Neo4j;
import org.entcore.directory.services.SubjectService;
import org.entcore.directory.Directory;
import io.vertx.core.eventbus.EventBus;

import static fr.wseduc.webutils.Utils.handlerToAsyncHandler;
import static org.entcore.common.neo4j.Neo4jResult.*;

public class DefaultSubjectService implements SubjectService {

    private final Neo4j neo = Neo4j.getInstance();
    private final EventBus eb;

    public DefaultSubjectService (EventBus eb) {
        this.eb = eb;
    }

    @Override
    public void getSubjects(String structureId, Handler<Either<String, JsonArray>> results) {
        final JsonObject params = new JsonObject().put("structureId", structureId);
        String query = "MATCH (s:Structure {id : {structureId}})<-[:SUBJECT]-(sub:Subject)" +
                "return sub.id as id, sub.code as code, sub.label as label, sub.source as source";

        neo.execute(query, params, validResultHandler(results));
    }

    @Override
    public void createManual (JsonObject subject, Handler<Either<String, JsonObject>> result) {
        JsonObject action = new JsonObject()
                .put("action", "manual-add-subject")
                .put("subject", subject);

        eb.request(Directory.FEEDER, action, handlerToAsyncHandler(validUniqueResultHandler(0, result)));

    }

    @Override
    public void updateManual(JsonObject subject, Handler<Either<String, JsonObject>> result) {
        JsonObject action = new JsonObject()
                .put("action", "manual-update-subject")
                .put("subject", subject);
        eb.request(Directory.FEEDER, action, handlerToAsyncHandler((validUniqueResultHandler(0, result))));
    }

    @Override
    public void deleteManual(String subjectId, Handler<Either<String, JsonObject>> result) {
        JsonObject action = new JsonObject()
                .put("action", "manual-delete-subject")
                .put("subjectId", subjectId);
        eb.request(Directory.FEEDER, action, handlerToAsyncHandler((validEmptyHandler(result))));
    }
}