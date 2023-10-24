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
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.entcore.common.email.impl.PostgresEmailBuilder;
import org.entcore.common.email.impl.PostgresEmailHelper;
import org.entcore.common.events.EventStore;
import org.entcore.common.events.impl.PostgresqlEventStoreFactory;
import org.entcore.infra.controllers.MailController;
import org.entcore.test.TestHelper;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.testcontainers.containers.PostgreSQLContainer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.UUID;

@RunWith(VertxUnitRunner.class)
public class MailTest {
  private static final TestHelper test = TestHelper.helper();

  @ClassRule
  public static PostgreSQLContainer<?> pgContainer = test.database().createPostgreSQLContainer().withInitScript("init_email.sql");
  static PostgresEmailHelper helper;
  static JsonObject postgresql;

  @BeforeClass
  public static void setUp(TestContext context) throws Exception {
    postgresql = new JsonObject().put("host", pgContainer.getHost()).put("database", pgContainer.getDatabaseName()).put("user", pgContainer.getUsername()).put("password", pgContainer.getPassword()).put("port", pgContainer.getMappedPort(5432));
    final JsonObject config = new JsonObject().put("postgresql", postgresql).put("platform", "test");
    final JsonObject moduleConfig = new JsonObject();
    //sender = new PostgresEmailSender(null, test.vertx(), moduleConfig, config, 0);
    helper = PostgresEmailHelper.create(test.vertx(), postgresql);
    test.vertx().sharedData().getLocalMap("server").put("event-store", config.toString());
    test.vertx().eventBus().localConsumer(PostgresEmailHelper.MAILER_ADDRESS, new MailController(test.vertx(), config));
  }

  PostgresEmailBuilder.EmailBuilder mail() {
    return PostgresEmailBuilder.mail().withPriority(0).withProfile("Teacher").withUserId("userid").withPlatformUrl("http://entcore.org").withPlatformId("platformid").withModule("infra").withBody("Test").withHeader("header", "value").withSubject("subject").withFrom("test@entcore.org").withTo("dest@entcore.org");
  }

  @Test
  public void testMailShouldCreate(TestContext context) {
    //multiple pgclient (eventstore) should not affect mailer
    final PostgresqlEventStoreFactory fac = new PostgresqlEventStoreFactory();
    fac.setVertx(test.vertx());
    final EventStore store = fac.getEventStore("test");
    final Async async = context.async();
    final PostgresEmailBuilder.EmailBuilder mail = mail();
    helper.createWithAttachments(mail, new ArrayList<>()).onComplete((r -> {
      if (r.failed()) {
        r.cause().printStackTrace();
      } else {
        helper.setRead((UUID) mail.getMail().get("id"), new JsonObject()).onComplete(r2 -> {
          if (r2.failed()) {
            r2.cause().printStackTrace();
          }
          context.assertTrue(r2.succeeded());
          async.complete();
        });
      }
      context.assertTrue(r.succeeded());
    }));
  }

  @Test
  public void testMailShouldCreateInsideWorker(TestContext testCtx) {
    //multiple pgclient (eventstore) should not affect mailer
    final PostgresqlEventStoreFactory fac = new PostgresqlEventStoreFactory();
    fac.setVertx(test.vertx());
    final EventStore store = fac.getEventStore("test");
    final Async async = testCtx.async();
    DeploymentOptions deplomentOptions = new DeploymentOptions()
        .setWorker(true).setInstances(1)
        .setIsolationGroup("mail_worker_group")
        .setIsolatedClasses(Collections.singletonList("org.entcore.infra.*"))
        .setConfig(new JsonObject().put("postgres", postgresql));
    test.vertx().deployVerticle("org.entcore.infra.MailWorkerForTest", deplomentOptions).onSuccess(rDep -> {
      test.vertx().eventBus().request(MailWorkerForTest.class.getSimpleName(),
          new JsonObject().put("action", "send"), r -> {
            if (r.failed()) {
              r.cause().printStackTrace();
            } else {
              final JsonObject json = (JsonObject) r.result().body();
              testCtx.assertTrue(json.getBoolean("success"));
            }
            async.complete();
          });
    });
  }
}
