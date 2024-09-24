package org.entcore.common.sql;

import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

public interface ISql {

  void prepared(String query, JsonArray values, Handler<Message<JsonObject>> handler);

  void prepared(String query, JsonArray values, DeliveryOptions deliveryOptions, Handler<Message<JsonObject>> handler);

  Future<Message<JsonObject>> prepared(String query, JsonArray values, DeliveryOptions deliveryOptions);

  void raw(String query, Handler<Message<JsonObject>> handler);

  void insert(String table, JsonObject params, Handler<Message<JsonObject>> handler);

  void insert(String table, JsonObject params, String returning, Handler<Message<JsonObject>> handler);

  void insert(String table, JsonArray fields, JsonArray values, Handler<Message<JsonObject>> handler);

  void insert(String table, JsonArray fields, JsonArray values, String returning,
              Handler<Message<JsonObject>> handler);

  void upsert(String table, JsonArray fields, JsonArray values, JsonArray conflictFields,
              JsonArray updateFields, String returning,
              Handler<Message<JsonObject>> handler);

  void select(String table, JsonArray fields, Handler<Message<JsonObject>> handler);

  void transaction(JsonArray statements, Handler<Message<JsonObject>> handler);

  void transaction(JsonArray statements, DeliveryOptions deliveryOptions, Handler<Message<JsonObject>> handler);
}
