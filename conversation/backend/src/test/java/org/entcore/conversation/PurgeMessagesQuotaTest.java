/*
 * Copyright © "Open Digital Education", 2026
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

package org.entcore.conversation;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import fr.wseduc.transformer.IContentTransformerClient;
import org.entcore.common.editor.IContentTransformerEventRecorder;
import org.entcore.common.sql.Sql;
import org.entcore.common.utils.Config;
import org.entcore.conversation.service.ConversationService;
import org.entcore.conversation.service.impl.SqlConversationService;
import org.entcore.test.TestHelper;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.testcontainers.containers.PostgreSQLContainer;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;

/**
 * The purge of old messages (IMPULS-5906) removes rows from the boxes of the users : like deleting them from the
 * application, it must give their storage back, otherwise the storage counter keeps counting them forever.
 */
@RunWith(VertxUnitRunner.class)
public class PurgeMessagesQuotaTest {

    private static final TestHelper test = TestHelper.helper();
    static final String SCHEMA = "conversation";

    @ClassRule
    public static PostgreSQLContainer<?> pgContainer = test.database().createPostgreSQLContainer();

    static ConversationService conversationService;
    /** Storage given back per user, as received by the fake workspace quota service. */
    static final Map<String, Long> released = new ConcurrentHashMap<>();

    @BeforeClass
    public static void setUp(TestContext context) throws Exception {
        Config.getInstance().setConfig(new JsonObject());
        conversationService = new SqlConversationService(test.vertx(), SCHEMA, IContentTransformerClient.noop,
                IContentTransformerEventRecorder.noop);
        // 027-fix-app-rights.sql grants default privileges for this role, which the test container does not have.
        pgContainer.execInContainer("psql", "-U", pgContainer.getUsername(), "-d", pgContainer.getDatabaseName(),
                "-c", "CREATE ROLE \"web-education\"");
        test.database().initPostgreSQL(context, pgContainer, SCHEMA);
        test.vertx().eventBus().<JsonObject>consumer("org.entcore.workspace.quota", message -> {
            final JsonObject body = message.body();
            if ("updateUserQuota".equals(body.getString("action"))) {
                released.merge(body.getString("userId"), body.getLong("size"), Long::sum);
            }
            message.reply(new JsonObject().put("storage", 0L));
        });
    }

    private Future<JsonArray> raw(String query) {
        final Promise<JsonArray> promise = Promise.promise();
        Sql.getInstance().raw(query, result -> {
            if ("ok".equals(result.body().getString("status"))) {
                promise.complete(result.body().getJsonArray("results", new JsonArray()));
            } else {
                promise.fail(result.body().getString("message", "sql error"));
            }
        });
        return promise.future();
    }

    @Test
    public void purgeShouldGiveBackTheStorageOfPurgedMessages(TestContext context) {
        final Async async = context.async();
        final long now = System.currentTimeMillis();
        // Ids are UUIDs since usermessages_v2.
        final String u1 = "00000000-0000-0000-0001-000000000001", u2 = "00000000-0000-0000-0001-000000000002",
                u3 = "00000000-0000-0000-0001-000000000003";
        final String old1 = "00000000-0000-0000-0002-000000000001", old2 = "00000000-0000-0000-0002-000000000002",
                filed = "00000000-0000-0000-0002-000000000003", recent = "00000000-0000-0000-0002-000000000004",
                persdir = "00000000-0000-0000-0002-000000000005";
        raw("INSERT INTO conversation.messages (id, body, \"from\", \"to\", state, date, subject) VALUES "
                + "('" + old1 + "', 'b', 'x', '[]', 'SENT', 0, 's'), ('" + old2 + "', 'b', 'x', '[]', 'SENT', 0, 's'), "
                + "('" + filed + "', 'b', 'x', '[]', 'SENT', 0, 's'), ('" + recent + "', 'b', 'x', '[]', 'SENT', " + now + ", 's'), "
                + "('" + persdir + "', 'b', 'x', '[]', 'SENT', 0, 'Message de persdir')")
            .compose(v -> raw("INSERT INTO conversation.folders (id, user_id, name) VALUES ('p-folder', '" + u1 + "', 'kept')"))
            .compose(v -> raw("INSERT INTO conversation.usermessages_v2 (user_id, message_id, folder_id, total_quota, msg_state, msg_date) VALUES "
                + "('" + u1 + "', '" + old1 + "', NULL, 100, 'SENT', 0), ('" + u1 + "', '" + old2 + "', NULL, 50, 'SENT', 0), "
                + "('" + u2 + "', '" + old1 + "', NULL, 30, 'SENT', 0), ('" + u3 + "', '" + old2 + "', NULL, 0, 'SENT', 0), "
                + "('" + u1 + "', '" + recent + "', NULL, 70, 'SENT', " + now + "), ('" + u1 + "', '" + filed + "', 'p-folder', 999, 'SENT', 0), "
                + "('" + u1 + "', '" + persdir + "', NULL, 10, 'SENT', 0)"))
            .compose(v -> conversationService.purgeMessages())
            .compose(v -> raw("SELECT message_id::text FROM conversation.usermessages_v2 ORDER BY 1"))
            .onSuccess(kept -> {
                context.assertEquals(new JsonArray().add(new JsonArray().add(filed)).add(new JsonArray().add(recent))
                        .add(new JsonArray().add(persdir)).encode(), kept.encode(),
                    "Only the recent message, the one filed in a folder and the protected supervision message must stay");
                context.assertEquals(-150L, released.get(u1), "u1 gets back the storage of its two purged messages");
                context.assertEquals(-30L, released.get(u2));
                context.assertFalse(released.containsKey(u3), "Nothing to give back when the purged rows weighed nothing");
                async.complete();
            })
            .onFailure(context::fail);
    }
}
