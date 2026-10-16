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

import java.util.ArrayList;
import java.util.Arrays;

import fr.wseduc.transformer.IContentTransformerClient;
import org.entcore.common.editor.IContentTransformerEventRecorder;
import org.entcore.common.sql.Sql;
import org.entcore.common.user.UserInfos;
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
 * Restoring a message from the trash after DeleteOrphan removed its thread (IMPULS-6262).
 * DeleteOrphan deletes a thread once every participant has trashed it, while its messages stay in the trashes.
 * The restore must recreate the thread before linking the user back to it : with the userthreads foreign key,
 * the link alone fails and rolls back the whole restore.
 */
@RunWith(VertxUnitRunner.class)
public class RestoreTest {

    private static final TestHelper test = TestHelper.helper();
    static final String schema = "conversation";

    @ClassRule
    public static PostgreSQLContainer<?> pgContainer = test.database().createPostgreSQLContainer();

    static ConversationService conversationService;

    @BeforeClass
    public static void setUp(TestContext context) throws Exception {
        Config.getInstance().setConfig(new JsonObject());
        conversationService = new SqlConversationService(test.vertx(), schema, IContentTransformerClient.noop,
                IContentTransformerEventRecorder.noop);
        // 027-fix-app-rights.sql grants default privileges for this role, which the test container does not have.
        pgContainer.execInContainer("psql", "-U", pgContainer.getUsername(), "-d", pgContainer.getDatabaseName(),
                "-c", "CREATE ROLE \"web-education\"");
        test.database().initPostgreSQL(context, pgContainer, schema);
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

    private Future<Void> restore(String userId, String... messageIds) {
        final Promise<Void> promise = Promise.promise();
        final UserInfos user = new UserInfos();
        user.setUserId(userId);
        conversationService.restore(new ArrayList<>(Arrays.asList(messageIds)), user, res -> {
            if (res.isRight()) {
                promise.complete();
            } else {
                promise.fail(res.left().getValue());
            }
        });
        return promise.future();
    }

    /** Two sent messages of the same thread, both in the trash of the user. The thread row itself is not created. */
    private Future<JsonArray> trashedThread(String prefix, String userId) {
        return raw("INSERT INTO conversation.messages (id, body, \"from\", \"to\", state, date, subject, thread_id) VALUES "
                + "('" + prefix + "-m1', 'b', 'sender', '[]', 'SENT', 1000, 'first', '" + prefix + "-t'),"
                + "('" + prefix + "-m2', 'b', 'sender', '[]', 'SENT', 2000, 'last', '" + prefix + "-t')")
            .compose(v -> raw("INSERT INTO conversation.usermessages (user_id, message_id, trashed) VALUES "
                + "('" + userId + "', '" + prefix + "-m1', true), ('" + userId + "', '" + prefix + "-m2', true)"));
    }

    private Future<JsonArray> thread(String id, long date, String subject) {
        return raw("INSERT INTO conversation.threads (id, date, subject, \"from\", \"to\") "
                + "VALUES ('" + id + "', " + date + ", '" + subject + "', 'sender', '[]')");
    }

    @Test
    public void restoreShouldRecreateThreadDeletedByDeleteOrphan(TestContext context) {
        final Async async = context.async();
        trashedThread("del", "del-u")
            .compose(v -> restore("del-u", "del-m1"))
            .compose(v -> raw("SELECT subject, date FROM conversation.threads WHERE id = 'del-t'"))
            .compose(thread -> {
                context.assertEquals(1, thread.size(), "The thread must be recreated");
                context.assertEquals("last", thread.getJsonArray(0).getString(0),
                    "The thread is rebuilt from its last sent message, as sending does");
                context.assertEquals(2000L, thread.getJsonArray(0).getLong(1));
                return raw("SELECT count(*) FROM conversation.userthreads WHERE user_id = 'del-u' AND thread_id = 'del-t'");
            })
            .compose(link -> {
                context.assertEquals(1, link.getJsonArray(0).getInteger(0), "The user must find the thread in its list");
                return raw("SELECT trashed FROM conversation.usermessages WHERE user_id = 'del-u' AND message_id = 'del-m1'");
            })
            .onSuccess(trashed -> {
                context.assertFalse(trashed.getJsonArray(0).getBoolean(0), "The message must be out of the trash");
                async.complete();
            })
            .onFailure(context::fail);
    }

    @Test
    public void restoreShouldLeaveExistingThreadUntouched(TestContext context) {
        final Async async = context.async();
        trashedThread("keep", "keep-u")
            .compose(v -> thread("keep-t", 5000, "current"))
            .compose(v -> restore("keep-u", "keep-m1"))
            .compose(v -> raw("SELECT subject, date FROM conversation.threads WHERE id = 'keep-t'"))
            .compose(thread -> {
                context.assertEquals("current", thread.getJsonArray(0).getString(0), "An existing thread is not rewritten");
                context.assertEquals(5000L, thread.getJsonArray(0).getLong(1));
                return raw("SELECT count(*) FROM conversation.userthreads WHERE user_id = 'keep-u' AND thread_id = 'keep-t'");
            })
            .onSuccess(link -> {
                context.assertEquals(1, link.getJsonArray(0).getInteger(0));
                async.complete();
            })
            .onFailure(context::fail);
    }

    /** One message on a deleted thread must not prevent restoring the others of the same selection. */
    @Test
    public void restoreShouldSucceedForMixedSelection(TestContext context) {
        final Async async = context.async();
        trashedThread("mix1", "mix-u")
            .compose(v -> trashedThread("mix2", "mix-u"))
            .compose(v -> thread("mix2-t", 2000, "last"))
            .compose(v -> restore("mix-u", "mix1-m2", "mix2-m2"))
            .compose(v -> raw("SELECT count(*) FROM conversation.usermessages WHERE user_id = 'mix-u' AND trashed = false"))
            .compose(restored -> {
                context.assertEquals(2, restored.getJsonArray(0).getInteger(0), "Both messages must be restored");
                return raw("SELECT count(*) FROM conversation.userthreads WHERE user_id = 'mix-u'");
            })
            .onSuccess(links -> {
                context.assertEquals(2, links.getJsonArray(0).getInteger(0), "Both threads must be back in the list");
                async.complete();
            })
            .onFailure(context::fail);
    }
}
