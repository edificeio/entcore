package com.wse.neo4j;

import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import org.neo4j.cypher.javacompat.ExecutionEngine;
import org.neo4j.cypher.javacompat.ExecutionResult;
import org.neo4j.geoff.Geoff;
import org.neo4j.geoff.Subgraph;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.PropertyContainer;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;
import org.neo4j.graphdb.factory.GraphDatabaseSettings;
import org.neo4j.graphdb.index.Index;
import org.neo4j.graphdb.index.IndexManager;
import org.vertx.java.busmods.BusModBase;
import org.vertx.java.core.Handler;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.JsonArray;
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
				.setConfig(GraphDatabaseSettings.node_keys_indexable, config.getObject("neo4j").getString("node_keys_indexable"))
				.setConfig(GraphDatabaseSettings.node_auto_indexing, config.getObject("neo4j").getString("node_auto_indexing"))
				.newGraphDatabase();

		engine = new ExecutionEngine(gdb);
		eb.registerHandler(config.getString("address"),this);
		logger.info("BusModBase: Neo4jPertistor  starts on address: " + config.getString("address"));
			}

	@Override
	public void stop() {
		super.stop();
		if (gdb != null)
			gdb.shutdown();
	}

	@Override
	public void handle(Message<JsonObject> m) {
		switch(m.body().getString("action")) {
			case "execute" :
				execute(m);
				break;
			case "executeMultiple" :
				executeMultiple(m);
				break;
			case "batch-insert" :
				batchInsert(m);
				break;
			default :
				sendError(m, "Invalid or missing action");
		}
	}

	private void execute (Message<JsonObject> m) {
		ExecutionResult result = null;
		try {
			if (m.body().toMap().containsKey("params")){
				result = engine.execute(m.body().getString("query"), m.body().getObject("params").toMap());
			} else {
				result = engine.execute(m.body().getString("query"));
			}
		} catch (Exception e) {
			sendError(m, e.getMessage());
			e.printStackTrace();
		}
		JsonObject json = toJson(result);
		sendOK(m, json);
	}

	private void batchInsert (Message<JsonObject> m) {
		Reader query = new StringReader(m.body().getString("query"));
		Map<String,PropertyContainer> result;
		try {
			result = Geoff.insertIntoNeo4j(new Subgraph(query), gdb, null);
			JsonObject joResult = new JsonObject().putNumber("insert", result.size());
			sendOK(m, joResult);
		} catch (Exception ex) {
			logger.error(ex.getMessage(), ex);
		}
	}

	private void executeMultiple (Message<JsonObject> m) {
		ExecutionResult result = null;
		try {
			List<Map<String, Object>> queryMap = new ArrayList<>();
			StringTokenizer reqTkn =
					new StringTokenizer(m.body().getString("queries"),m.body().getString("requestSeparator"));
			while (reqTkn.hasMoreElements()) {
				Map<String, Object> attrMap = new HashMap<>();
				StringTokenizer attrTkn =
						new StringTokenizer(reqTkn.nextToken(),m.body().getString("attrSeparator"));
				while (attrTkn.hasMoreElements()) {
					String token = attrTkn.nextToken();
					String[] attrArray = token.split(m.body().getString("valueSeparator"));
					if (attrArray.length > 1) {
						// TODO : gestion multivalue
						attrMap.put(attrArray[0],attrArray[1]);
					}
				}
				queryMap.add(attrMap);
			}

			Map<String, Object> params = new HashMap<>();
			params.put("props", queryMap);
			result = engine.execute("create (n {props}) return n", params);
		} catch (Exception e) {
			sendError(m, e.getMessage());
			e.printStackTrace();
		}
		JsonObject json = toJson(result);
		sendOK(m, json);
	}

	@SuppressWarnings("unchecked")
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
				Object v = column.getValue();
				if (v instanceof Iterable) {
					jsonRow.putArray(column.getKey(), new JsonArray((List<Object>) v));
				} else {
					String value = (v == null) ? "" : v.toString();
					jsonRow.putString(column.getKey(), value);
				}
			}
		}
		return new JsonObject().putObject("result", json) ;
	}

}
