/*
 * Copyright. Tous droits réservés. WebServices pour l’Education.
 */

package org.entcore.feeder.aaf;

import org.vertx.java.core.Handler;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.JsonObject;


public interface ImportProcessing {

	void start(Handler<Message<JsonObject>> handler);

	String getMappingResource();

	void process(JsonObject object);

}
