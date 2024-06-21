 /*
  * Copyright © "Open Digital Education", 2019
  *
  * This program is published by "Open Digital Education".
  * You must indicate the name of the software and the company in any production /contribution
  * using the software and indicate on the home page of the software industry in question,
  * "powered by Open Digital Education" with a reference to the website: https://opendigitaleducation.com/.
  *
  * This program is free software, licensed under the terms of the GNU Affero General Public License
  * as published by the Free Software Foundation, version 3 of the License.
  *
  * You can redistribute this application and/or modify it since you respect the terms of the GNU Affero General Public License.
  * If you modify the source code and then use this modified source code in your creation, you must make available the source code of your modifications.
  *
  * You should have received a copy of the GNU Affero General Public License along with the software.
  * If not, please see : <http://www.gnu.org/licenses/>. Full compliance requires reading the terms of this license and following its directives.
  *
  * This program is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
  */

 package org.entcore.infra;

 import io.vertx.core.DeploymentOptions;
 import io.vertx.core.Handler;
 import io.vertx.core.eventbus.Message;
 import io.vertx.core.json.JsonObject;
 import io.vertx.ext.unit.Async;
 import io.vertx.ext.unit.TestContext;
 import io.vertx.ext.unit.junit.VertxUnitRunner;
 import org.entcore.common.events.EventStore;
 import org.entcore.common.events.EventStoreFactory;
 import org.entcore.common.events.impl.BusEventStoreFactory;
 import org.entcore.common.events.impl.PostgresqlEventStoreFactory;
 import org.entcore.test.TestHelper;
 import org.junit.BeforeClass;
 import org.junit.ClassRule;
 import org.junit.Test;
 import org.junit.runner.RunWith;
 import org.testcontainers.containers.MongoDBContainer;
 import org.testcontainers.containers.PostgreSQLContainer;

 import java.util.Arrays;

 @RunWith(VertxUnitRunner.class)
 public class EventStoreTest {
     private static final TestHelper test = TestHelper.helper();

     @ClassRule
     public static PostgreSQLContainer<?> pgContainer = test.database().createPostgreSQLContainer().withInitScript("init_event.sql");
     @ClassRule
     public static MongoDBContainer mongoContainer = test.database().createMongoContainer();
     static EventStoreFactory storeFactory;
     static JsonObject postgresql;
     static EventStore eventStore;
     @BeforeClass
     public static void setUp(TestContext context) throws Exception {
         test.database().initMongo(context, mongoContainer);
         postgresql = new JsonObject().put("host", pgContainer.getHost()).put("database", pgContainer.getDatabaseName()).put("user", pgContainer.getUsername()).put("password", pgContainer.getPassword()).put("port", pgContainer.getMappedPort(5432));
         final JsonObject config = new JsonObject().put("postgresql", postgresql).put("platform", "test");
         test.vertx().sharedData().getLocalMap("server").put("event-store", config.toString());
         final EventStoreFactory fac = new PostgresqlEventStoreFactory();
         fac.setVertx(test.vertx());
         eventStore = fac.getEventStore("test");
         test.vertx().eventBus().localConsumer("event.store", new Handler<Message<JsonObject>>() {
             @Override
             public void handle(Message<JsonObject> event) {
                 final JsonObject json = event.body();
                 final String eventType = event.body().getString("event-type");
                 json.put("user_id", json.getString("userId"));
                 json.remove("event-type");
                 json.remove("userId");
                 json.remove("groups");
                 eventStore.storeCustomEvent(eventType, json);
             }
         });
     }

     @Test
     public void testShouldCreateDefaultEventStore(TestContext context) {
         EventStoreFactory factory = EventStoreFactory.getFactory();
         context.assertFalse(factory instanceof BusEventStoreFactory);
     }

     @Test
     public void testShouldCreateEvent(TestContext context) {
         final Async async = context.async();
         final EventStoreFactory fac = new PostgresqlEventStoreFactory();
         fac.setVertx(test.vertx());
         final EventStore store = fac.getEventStore("test");
         //multiple pgclient (eventstore) should not work inside worker
         //TODO missing handler on eventstore interface
         test.vertx().setTimer(500, r->{
             store.createAndStoreEvent("ACCESS", TestHelper.helper().directory().generateUser("user1"));
             async.complete();
         });
     }

     @Test
     public void testShouldCreateEventInsideWorker(TestContext testCtx) {
         //multiple pgclient (eventstore) should not work inside worker
         final Async async = testCtx.async();
         final EventStoreFactory fac = new PostgresqlEventStoreFactory();
         fac.setVertx(test.vertx());
         final EventStore store = fac.getEventStore("test");
         final DeploymentOptions deploymentOptions = new DeploymentOptions().setWorker(true).setInstances(1)
             .setConfig(new JsonObject().put("postgres", postgresql))
             .setIsolationGroup("event_worker_group")
             .setIsolatedClasses(Arrays.asList("org.entcore.infra.*"));
         test.vertx().deployVerticle("org.entcore.infra.EventWorkerForTest", deploymentOptions)
         .onSuccess(rDep -> {
             test.vertx().eventBus().request(EventWorkerForTest.class.getSimpleName(),
                     new JsonObject().put("action", "send"), r -> {
                         if (r.failed()) {
                             r.cause().printStackTrace();
                         } else {
                             final JsonObject json = (JsonObject)r.result().body();
                             testCtx.assertTrue(json.getBoolean("success"));
                         }
                         async.complete();
                     });
         });
     }

 }
