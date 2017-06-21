package org.entcore.directory.services.impl;

import com.mongodb.QueryBuilder;
import fr.wseduc.mongodb.MongoDb;
import fr.wseduc.mongodb.MongoQueryBuilder;
import fr.wseduc.mongodb.MongoUpdateBuilder;
import fr.wseduc.webutils.Either;
import org.entcore.common.service.impl.MongoDbCrudService;
import org.entcore.directory.pojo.Slot;
import org.entcore.directory.services.SlotProfileService;
import org.vertx.java.core.Handler;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;

import java.util.UUID;

import static org.entcore.common.mongodb.MongoDbResult.*;


public class DefaultSlotProfileService extends MongoDbCrudService implements SlotProfileService {


    public DefaultSlotProfileService(String collection) {
        super(collection);
    }

    @Override
    public void createSlot(String idSlotProfile, JsonObject slot, Handler<Either<String, JsonObject>> handler) {
        // Query
        QueryBuilder query = QueryBuilder.start("_id").is(idSlotProfile);

        // Set UUID
        slot.putString(Slot.ID, UUID.randomUUID().toString());

        // Modifier
        MongoUpdateBuilder modifier = new MongoUpdateBuilder();
        modifier.push("slots", slot);
        modifier.set("modified", MongoDb.now());
        mongo.update(this.collection, MongoQueryBuilder.build(query), modifier.build(), validActionResultHandler(handler));
    }

    @Override
    public void listSlots(String idSlotProfile, Handler<Either<String, JsonObject>> handler) {
        // Query
        QueryBuilder query = QueryBuilder.start("_id").is(idSlotProfile);

        // Projection
        JsonObject projection = new JsonObject().putNumber("slots", 1).putNumber("_id", 0);
        mongo.findOne(this.collection, MongoQueryBuilder.build(query), projection, validResultHandler(handler));
    }

    @Override
    public void listSlotProfilesByStructure(String structureId, Handler<Either<String, JsonArray>> handler) {
        // Query
        JsonObject match = new JsonObject().putString("schoolId", structureId);

        JsonObject sort = new JsonObject().putNumber("name", 1);

        // Projection
        JsonObject projection = new JsonObject()
                .putNumber("name", 1)
                .putNumber("schoolId", 1)
                .putNumber("slots", 1);
        mongo.find(this.collection, match, sort, projection, validResultsHandler(handler));
    }

    @Override
    public void updateSlot(String idSlotProfile, JsonObject slot, Handler<Either<String, JsonObject>> handler) {
        // Query
        QueryBuilder query = QueryBuilder.start("_id").is(idSlotProfile);
        query = query.and("slots").elemMatch(QueryBuilder.start(Slot.ID).is(slot.getString(Slot.ID)).get());

        // Modifier
        MongoUpdateBuilder modifier = new MongoUpdateBuilder();
        modifier.set("slots.$", slot);
        modifier.set("modified", MongoDb.now());
        mongo.update(this.collection, MongoQueryBuilder.build(query), modifier.build(), validActionResultHandler(handler));
    }

    @Override
    public void deleteSlotFromSlotProfile(String idSlotProfile, String idSlot, Handler<Either<String, JsonObject>> handler) {
        // Query
        QueryBuilder query = QueryBuilder.start("_id").is(idSlotProfile);

        // Modifier
        MongoUpdateBuilder modifier = new MongoUpdateBuilder();
        modifier.pull("slots", MongoQueryBuilder.build(QueryBuilder.start(Slot.ID).is(idSlot)));
        modifier.set("modified", MongoDb.now());
        mongo.update(this.collection, MongoQueryBuilder.build(query), modifier.build(), validActionResultHandler(handler));
    }

    @Override
    public void updateSlotProfile(String idSlotProfile, String name, Handler<Either<String, JsonObject>> handler) {
        // Query
        QueryBuilder query = QueryBuilder.start("_id").is(idSlotProfile);

        // Modifier
        MongoUpdateBuilder modifier = new MongoUpdateBuilder();
        modifier.set("name", name);
        modifier.set("modified", MongoDb.now());
        mongo.update(this.collection, MongoQueryBuilder.build(query), modifier.build(), validActionResultHandler(handler));
    }
}
