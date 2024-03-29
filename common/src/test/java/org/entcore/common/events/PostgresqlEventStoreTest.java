/*
 * Copyright © "Open Digital Education", 2020
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

package org.entcore.common.events;

import static org.junit.Assert.assertTrue;

import java.util.UUID;

import io.vertx.pgclient.PgConnectOptions;
import io.vertx.pgclient.PgConnection;
import io.vertx.pgclient.PgPool;
import org.entcore.common.events.impl.PostgresqlEventStore;
import org.entcore.common.events.impl.PostgresqlEventStoreFactory;
import org.entcore.common.user.UserInfos;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.testcontainers.containers.PostgreSQLContainer;

import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.shareddata.LocalMap;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;

@RunWith(VertxUnitRunner.class)
public class PostgresqlEventStoreTest {
    @ClassRule
    public static PostgreSQLContainer<?> postgreSQLContainer = new PostgreSQLContainer("postgres:9.5");

    private static final JsonObject eventStoreTestConfigJson = new JsonObject().put("platform",
            "6449e6ea-1d6b-49b8-9245-46c88b167178");
    private Vertx vertx;

    @BeforeClass
    public static void beforeAll(TestContext context) {
        final JsonObject postresql = new JsonObject().put("database", postgreSQLContainer.getDatabaseName())
                .put("port", postgreSQLContainer.getMappedPort(PostgreSQLContainer.POSTGRESQL_PORT))
                .put("host", postgreSQLContainer.getContainerIpAddress())
                .put("password", postgreSQLContainer.getPassword()).put("user", postgreSQLContainer.getUsername());
        eventStoreTestConfigJson.put("postgresql", postresql);
        final Async async = context.async();
        final PgConnectOptions options = new PgConnectOptions().setPort(postresql.getInteger("port", 5432))
                .setHost(postresql.getString("host")).setDatabase(postresql.getString("database"))
                .setUser(postresql.getString("user")).setPassword(postresql.getString("password"));
        PgConnection.connect(Vertx.vertx(), options, res -> {
            context.assertTrue(res.succeeded());
            res.result().query("CREATE SCHEMA events").execute(resSch -> {
                context.assertTrue(resSch.succeeded());
                res.result().query("CREATE TABLE events.login_events(id VARCHAR(36) PRIMARY KEY)").execute(resSql -> {
                    context.assertTrue(resSql.succeeded());
                    async.complete();
                });
            });
        });
    }

    @Before
    public void setUp(TestContext context) {

        Vertx vertx = Vertx.vertx();
        final LocalMap<Object, Object> serverMap = vertx.sharedData().getLocalMap("server");
        serverMap.put("event-store", eventStoreTestConfigJson.encode());
        this.vertx = vertx;
        vertx.eventBus().localConsumer("event.blacklist", ar -> {
            ar.reply(new JsonArray());
        });
    }

    @Test
    public void testKnownEvents(TestContext context) {
        Async async = context.async();
        final PostgresqlEventStoreFactory postgresqlEventStoreFactory = new PostgresqlEventStoreFactory();
        postgresqlEventStoreFactory.setVertx(vertx);
        postgresqlEventStoreFactory.getEventStore("Auth", ar -> {
            if (ar.succeeded()) {
                PostgresqlEventStore eventStore = (PostgresqlEventStore) ar.result();
                assertTrue(eventStore.getKnownEvents().contains("LOGIN"));
                async.complete();
            } else {
                context.fail(ar.cause());
            }
        });
    }

    @Test
    public void testInsertEvents(TestContext context) {
        Async async = context.async();
        final PostgresqlEventStoreFactory postgresqlEventStoreFactory = new PostgresqlEventStoreFactory();
        postgresqlEventStoreFactory.setVertx(vertx);
        postgresqlEventStoreFactory.getEventStore("Auth", ar -> {
            if (ar.succeeded()) {
                final PostgresqlEventStore eventStore = (PostgresqlEventStore) ar.result();
                final UserInfos user = new UserInfos();
                user.setUserId(UUID.randomUUID().toString());
                user.setType("Teacher");
                eventStore.createAndStoreEvent("LOGIN", user, new JsonObject());
                async.complete();
            } else {
                context.fail(ar.cause());
            }
        });
    }

    @Test
    public void testInsertUnknownEvents(TestContext context) {
        Async async = context.async();
        final PostgresqlEventStoreFactory postgresqlEventStoreFactory = new PostgresqlEventStoreFactory();
        postgresqlEventStoreFactory.setVertx(vertx);
        postgresqlEventStoreFactory.getEventStore("Auth", ar -> {
            if (ar.succeeded()) {
                final PostgresqlEventStore eventStore = (PostgresqlEventStore) ar.result();
                final UserInfos user = new UserInfos();
                user.setUserId(UUID.randomUUID().toString());
                user.setType("Student");
                eventStore.createAndStoreEvent("BLA", user, new JsonObject().put("blip", "blop"));
                async.complete();
            } else {
                context.fail(ar.cause());
            }
        });
    }

}