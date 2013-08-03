package com.wse.neo4j;

import java.net.URI;
import java.net.URISyntaxException;

import org.vertx.java.busmods.BusModBase;
import org.vertx.java.core.Handler;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.JsonObject;


public class Neo4jPersistor extends BusModBase implements Handler<Message<JsonObject>> {

	private GraphDatabase db;

	// TODO : write a config loader that merge standard conf with mode (dev,test,prod)) conf
	@Override
	public void start() {
		super.start();
		String serverUri = config.getString("server-uri");
		if (serverUri != null && !serverUri.trim().isEmpty()) {
			try {
				db = new Neo4jRest(new URI(serverUri), vertx, logger,
						config.getInteger("poolsize", 32));
			} catch (URISyntaxException e) {
				logger.error(e.getMessage(), e);
			}
		} else {
			db = new Neo4jEmbedded(config, logger);
		}

		eb.registerHandler(config.getString("address"),this);
		logger.info("BusModBase: Neo4jPertistor  starts on address: " + config.getString("address"));
	}

	@Override
	public void stop() {
		super.stop();
		if (db != null) {
			db.close();
		}
	}

	@Override
	public void handle(Message<JsonObject> m) {
		switch(m.body().getString("action")) {
			case "execute" :
				execute(m);
				break;
			case "executeBatch" :
				executeBatch(m);
				break;
			case "executeMultiple" :
				db.executeMultiple(m);
				break;
			case "batch-insert" :
				batchInsert(m);
				break;
			default :
				sendError(m, "Invalid or missing action");
		}
	}

	private void executeBatch(Message<JsonObject> m) {
		db.executeBatch(m.body().getArray("queries"), resultHandler(m));
	}

	private void execute(final Message<JsonObject> m) {
		db.execute(m.body().getString("query"), m.body().getObject("params"), resultHandler(m));
	}

	private void batchInsert(Message<JsonObject> m) {
		db.batchInsert(m.body().getString("query"), resultHandler(m));
	}

	private Handler<JsonObject> resultHandler(final Message<JsonObject> m) {
		return new Handler<JsonObject>() {

			@Override
			public void handle(JsonObject res) {
				String error = res.getString("message");
				if (error == null) {
					sendOK(m, res);
				} else {
					logger.error(res.getString("exception") + " : " + error);// "\\n" +
							//res.getArray("stacktrace").encode());
					sendError(m, error);
				}
			}
		};
	}

}
