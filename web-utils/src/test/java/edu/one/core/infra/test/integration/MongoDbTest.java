package edu.one.core.infra.test.integration;

import static org.vertx.testtools.VertxAssert.assertEquals;
import static org.vertx.testtools.VertxAssert.testComplete;

import org.junit.Test;
import org.vertx.java.core.AsyncResult;
import org.vertx.java.core.AsyncResultHandler;
import org.vertx.java.core.Handler;
import org.vertx.java.core.eventbus.EventBus;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.JsonObject;
import org.vertx.testtools.TestVerticle;

import edu.one.core.infra.MongoDb;

public class MongoDbTest extends TestVerticle {

	private MongoDb mongo;

	@Override
	public void start() {
		EventBus eb = vertx.eventBus();
		JsonObject config = new JsonObject();
		config.putString("address", "test.persistor");
		config.putString("db_name", System.getProperty("vertx.mongo.database", "test_db"));
		config.putString("host", System.getProperty("vertx.mongo.host", "localhost"));
		config.putNumber("port", Integer.valueOf(System.getProperty("vertx.mongo.port", "27017")));
		String username = System.getProperty("vertx.mongo.username");
		String password = System.getProperty("vertx.mongo.password");
		if (username != null) {
			config.putString("username", username);
			config.putString("password", password);
		}
		config.putBoolean("fake", false);
		container.deployModule("io.vertx~mod-mongo-persistor~2.0.0-CR1", config, 1, new AsyncResultHandler<String>() {
			public void handle(AsyncResult<String> ar) {
				if (ar.succeeded()) {
					MongoDbTest.super.start();
				} else {
					ar.cause().printStackTrace();
				}
			}
		});
		mongo = new MongoDb(eb, "test.persistor");
	}

	@Test
	public void testPersistor() throws Exception {
		JsonObject document = new JsonObject();
		document.putString("content", "blip");
		mongo.save("test", document, new Handler<Message<JsonObject>>() {
			@Override
			public void handle(Message<JsonObject> msg) {
				String id = msg.body().getString("_id");
				String query = "{\"_id\":\"" + id + "\"}";
				String set = "{\"$set\": { \"content\": \"blop\"}}";
				mongo.update("test", new JsonObject(query), new JsonObject(set));
				mongo.findOne("test", new JsonObject(query), new Handler<Message<JsonObject>>() {
					@Override
					public void handle(Message<JsonObject> res) {
						container.logger().info(res.body().toString());
						assertEquals("blop", res.body().getObject("result").getString("content"));
						testComplete();
					}
				});
			}
		});
	}

	@Test
	public void testCommand() throws Exception {
		mongo.command("{ping:1}", new Handler<Message<JsonObject>>() {
			public void handle(Message<JsonObject> reply) {
				Number ok = reply.body().getObject("result").getNumber("ok");
				assertEquals(0.0, ok);
				testComplete();
			}
		});
	}

}
