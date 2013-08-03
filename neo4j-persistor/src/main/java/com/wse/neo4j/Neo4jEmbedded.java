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
import org.neo4j.graphdb.PropertyContainer;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;
import org.neo4j.graphdb.factory.GraphDatabaseSettings;
import org.vertx.java.core.Handler;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.logging.Logger;

import com.wse.neo4j.exception.ExceptionUtils;

public class Neo4jEmbedded implements GraphDatabase {

	private final GraphDatabaseService gdb;
	private final ExecutionEngine engine;
	private final Logger logger;

	public Neo4jEmbedded(JsonObject config, Logger logger) {
		gdb = new GraphDatabaseFactory()
		.newEmbeddedDatabaseBuilder(config.getString("datastore-path"))
		.setConfig(GraphDatabaseSettings.node_keys_indexable, config.getObject("neo4j").getString("node_keys_indexable"))
		.setConfig(GraphDatabaseSettings.node_auto_indexing, config.getObject("neo4j").getString("node_auto_indexing"))
		.newGraphDatabase();
		engine = new ExecutionEngine(gdb);
		this.logger = logger;
	}

	@Override
	public void execute(String query, JsonObject params, Handler<JsonObject> handler) {
		ExecutionResult result = null;
		try {
			if (params != null){
				result = engine.execute(query, params.toMap());
			} else {
				result = engine.execute(query);
			}
		} catch (Exception e) {
			handler.handle(ExceptionUtils.exceptionToJson(e));
		}
		JsonObject json = toJson(result);
		handler.handle(json);
	}

	@Override
	public void executeBatch(JsonArray queries, Handler<JsonObject> handler) {
		ExecutionResult result = null;
		JsonArray results = new JsonArray();
		try {
			int i = 0;
			for (Object q: queries) {
				JsonObject qr = (JsonObject) q;
				String query = qr.getString("query");
				JsonObject params = qr.getObject("params");
				if (params != null){
					result = engine.execute(query, params.toMap());
				} else {
					result = engine.execute(query);
				}
				results.addObject(toJson(result).putNumber("idx", i++));
			}
		} catch (Exception e) {
			handler.handle(ExceptionUtils.exceptionToJson(e));
		}
		JsonObject json = new JsonObject().putArray("results", results);
		handler.handle(json);
	}

	@Override
	public void batchInsert (String q, Handler<JsonObject> handler) {
		Reader query = new StringReader(q);
		Map<String,PropertyContainer> result;
		try {
			result = Geoff.insertIntoNeo4j(new Subgraph(query), gdb, null);
			JsonObject joResult = new JsonObject().putNumber("insert", result.size());
			handler.handle(joResult);
		} catch (Exception ex) {
			handler.handle(ExceptionUtils.exceptionToJson(ex));
		}
	}

	@Override
	public void executeMultiple (Message<JsonObject> m) {
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

	@Override
	public void close() {
		if (gdb != null) {
			gdb.shutdown();
		}
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



	  protected void sendOK(Message<JsonObject> message) {
		    sendOK(message, null);
		  }

		  protected void sendStatus(String status, Message<JsonObject> message) {
		    sendStatus(status, message, null);
		  }

		  protected void sendStatus(String status, Message<JsonObject> message, JsonObject json) {
		    if (json == null) {
		      json = new JsonObject();
		    }
		    json.putString("status", status);
		    message.reply(json);
		  }

		  protected void sendOK(Message<JsonObject> message, JsonObject json) {
		    sendStatus("ok", message, json);
		  }

		  protected void sendError(Message<JsonObject> message, String error) {
		    sendError(message, error, null);
		  }

		  protected void sendError(Message<JsonObject> message, String error, Exception e) {
		    logger.error(error, e);
		    JsonObject json = new JsonObject().putString("status", "error").putString("message", error);
		    message.reply(json);
		  }

}
