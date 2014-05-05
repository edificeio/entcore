/*
 * Copyright. Tous droits réservés. WebServices pour l’Education.
 */

package org.entcore.feeder;

import org.entcore.feeder.dictionary.structures.Importer;
import org.vertx.java.core.Handler;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.JsonObject;

public interface Feed {

	void launch(Importer importer, Handler<Message<JsonObject>> handler) throws Exception;

}
