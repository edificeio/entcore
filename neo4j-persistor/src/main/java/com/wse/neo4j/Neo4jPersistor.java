package com.wse.neo4j;

import java.util.Map;
import org.neo4j.cypher.javacompat.ExecutionEngine;
import org.neo4j.cypher.javacompat.ExecutionResult;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;
import org.neo4j.graphdb.factory.GraphDatabaseSettings;
import org.vertx.java.busmods.BusModBase;
import org.vertx.java.core.Handler;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.JsonObject;


public class Neo4jPersistor extends BusModBase implements Handler<Message<JsonObject>> {

	private GraphDatabaseService gdb;
	ExecutionEngine engine;

	// TODO : write a config loader that merge standard conf with mode (dev,test,prod)) conf
	@Override
	public void start() {
		super.start();
		gdb = new GraphDatabaseFactory()
				.newEmbeddedDatabaseBuilder(config.getString("datastore-path"))
				.setConfig(GraphDatabaseSettings.node_keys_indexable, config.getString("node_keys_indexable"))
				.setConfig(GraphDatabaseSettings.node_auto_indexing, config.getString("node_auto_indexing"))
				.newGraphDatabase();

		engine = new ExecutionEngine(gdb);
		eb.registerHandler(config.getString("address"),this);
		logger.info("BusModBase: Neo4jPertistor  starts on address: " + config.getString("address"));
	}

	@Override
	public void stop() throws Exception {
		super.stop();
		if (gdb != null) 
			gdb.shutdown();
	}

	@Override
	public void handle(Message<JsonObject> m) {
		switch(m.body.getString("action")) {
			case "execute" :
				execute(m);
				break;
			default :
				sendError(m, "Invalid or missing action");
		}
	}

	private void execute (Message<JsonObject> m) {
		ExecutionResult result = null;
		try {
			result = engine.execute(m.body.getString("query"));
		} catch (Exception e) {
			sendError(m, e.getMessage());
			e.printStackTrace();
		}
		JsonObject json = toJson(result);
		sendOK(m, json);
	}

	private JsonObject toJson (ExecutionResult result) {
		JsonObject json = new JsonObject();
		// TODO avoid "if null programming"
		if (result == null) {
			return json;
		}
		int i = 0;
		for (Map<String, Object> row : result) {
				JsonObject jsonRow = new JsonObject();
				json.putObject(String.valueOf(i++), jsonRow);
			for (Map.Entry<String, Object> column : row.entrySet()) {
				jsonRow.putString(column.getKey(), column.getValue().toString());
			}
		}
		return new JsonObject().putObject("result", json) ;
	}
}
