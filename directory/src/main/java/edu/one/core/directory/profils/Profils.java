package edu.one.core.directory.profils;

import org.vertx.java.core.Handler;
import org.vertx.java.core.json.JsonObject;

public interface Profils {

	void listGroupsProfils(Object [] typeFilter, String schoolId, Handler<JsonObject> handler);

}
