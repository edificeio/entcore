package edu.one.core.infra.test.integration;

import static org.vertx.testtools.VertxAssert.assertEquals;
import static org.vertx.testtools.VertxAssert.testComplete;

import org.junit.Test;
import org.vertx.java.core.AsyncResult;
import org.vertx.java.core.AsyncResultHandler;
import org.vertx.java.core.Handler;
import org.vertx.java.core.eventbus.EventBus;
import org.vertx.java.core.json.JsonObject;
import org.vertx.testtools.TestVerticle;

import edu.one.core.infra.Server;
import edu.one.core.infra.security.UserUtils;
import edu.one.core.infra.security.resources.UserInfos;

public class UserUtilsTest extends TestVerticle {

	private EventBus eb;

	@Override
	public void start() {
		eb = Server.getEventBus(vertx);

		JsonObject config = new JsonObject()
		.putString("server-uri", "http://localhost:7474/db/data/")
		.putNumber("poolsize", 1)
		.putString("address", "wse.neo4j.persistor");

		container.deployModule("com.wse.neo4j~neo4jPersistor~1.0.0", config, 1, new AsyncResultHandler<String>() {
			public void handle(AsyncResult<String> ar) {
				if (ar.succeeded()) {
					UserUtilsTest.super.start();
				} else {
					ar.cause().printStackTrace();
				}
			}
		});
	}

//	@Test
//	public void generateUserInfos() throws Exception {
//		UserUtils.generateUserInfos(eb, "4420000018", new Handler<UserInfos>() {
//
//			@Override
//			public void handle(UserInfos user) {
//				assertEquals("Audrey", user.getFirstName());
//				assertEquals("DULOUD", user.getLastName());
//				assertEquals("Audrey DULOUD", user.getUsername());
//				assertEquals("4400000002$ORDINAIRE$CM2 de Mme Rousseau", user.getClassId());
//				assertEquals(24, user.getAuthorizedActions().size());
//				testComplete();
//			}
//		});
//	}

}
