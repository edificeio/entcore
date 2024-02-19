package org.entcore.audience.reaction.service.impl;

import io.vertx.core.Handler;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.entcore.common.sql.ISql;

public class DumbSQL implements ISql {
  @Override
  public void prepared(String query, JsonArray values, Handler<Message<JsonObject>> handler) {

  }

  @Override
  public void prepared(String query, JsonArray values, DeliveryOptions deliveryOptions, Handler<Message<JsonObject>> handler) {

  }

  @Override
  public void raw(String query, Handler<Message<JsonObject>> handler) {

  }

  @Override
  public void insert(String table, JsonObject params, Handler<Message<JsonObject>> handler) {

  }

  @Override
  public void insert(String table, JsonObject params, String returning, Handler<Message<JsonObject>> handler) {

  }

  @Override
  public void insert(String table, JsonArray fields, JsonArray values, Handler<Message<JsonObject>> handler) {

  }

  @Override
  public void insert(String table, JsonArray fields, JsonArray values, String returning, Handler<Message<JsonObject>> handler) {

  }

  @Override
  public void upsert(String table, JsonArray fields, JsonArray values, JsonArray conflictFields, JsonArray updateFields, String returning, Handler<Message<JsonObject>> handler) {

  }

  @Override
  public void select(String table, JsonArray fields, Handler<Message<JsonObject>> handler) {

  }

  @Override
  public void transaction(JsonArray statements, Handler<Message<JsonObject>> handler) {

  }

  @Override
  public void transaction(JsonArray statements, DeliveryOptions deliveryOptions, Handler<Message<JsonObject>> handler) {

  }
}
