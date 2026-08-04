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
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;

/**
 * Covers the purge of the anti-spam guard rows : what it drops, and above all what it must keep, since a row
 * still within the retention window can be the one blocking a second reply on the same day.
 */
@RunWith(VertxUnitRunner.class)
public class AbsenceRepliesPurgeTest {

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
        test.database().initPostgreSQL(context, pgContainer, schema);
    }

    private Future<Void> raw(String query) {
        final Promise<Void> promise = Promise.promise();
        Sql.getInstance().raw(query, result -> {
            if ("ok".equals(result.body().getString("status"))) {
                promise.complete();
            } else {
                promise.fail(result.body().getString("message", "sql error"));
            }
        });
        return promise.future();
    }

    private Future<Integer> countGuardRows() {
        final Promise<Integer> promise = Promise.promise();
        Sql.getInstance().raw("SELECT count(*) AS nb FROM conversation.absence_replies", result -> {
            if ("ok".equals(result.body().getString("status"))) {
                promise.complete(result.body().getJsonArray("results").getJsonArray(0).getInteger(0));
            } else {
                promise.fail(result.body().getString("message", "sql error"));
            }
        });
        return promise.future();
    }

    private Future<Boolean> guardRowExists(String absentUserId) {
        final Promise<Boolean> promise = Promise.promise();
        Sql.getInstance().raw("SELECT count(*) AS nb FROM conversation.absence_replies WHERE absent_user_id = '"
                + absentUserId + "'", result -> {
            if ("ok".equals(result.body().getString("status"))) {
                promise.complete(result.body().getJsonArray("results").getJsonArray(0).getInteger(0) > 0);
            } else {
                promise.fail(result.body().getString("message", "sql error"));
            }
        });
        return promise.future();
    }

    /**
     * Default retention is seven days, so the thirty and ten day old rows go and the two hour old one stays.
     * The row just inside the window is the one that matters : dropping it would hand its expeditor a second
     * reply on the same day.
     */
    @Test
    public void purgeShouldDropOnlyRowsBeyondRetention(TestContext context) {
        final Async async = context.async();
        raw("DELETE FROM conversation.absence_replies")
            .compose(v -> raw("INSERT INTO conversation.absence_replies(absent_user_id, sender_id, last_sent_at) VALUES "
                    + "('purge-30d','s1', now() - interval '30 days'),"
                    + "('purge-10d','s2', now() - interval '10 days'),"
                    + "('purge-6d','s3',  now() - interval '6 days'),"
                    + "('purge-now','s4', now())"))
            .compose(v -> conversationService.purgeAbsenceReplies())
            .compose(v -> countGuardRows())
            .compose(remaining -> {
                context.assertEquals(2, remaining, "Only the rows within the retention window must remain");
                return guardRowExists("purge-6d");
            })
            .compose(sixDaysKept -> {
                context.assertTrue(sixDaysKept, "A six day old row is still within the seven day retention");
                return guardRowExists("purge-now");
            })
            .compose(todayKept -> {
                context.assertTrue(todayKept, "Today's row must never be dropped, it is what blocks a second reply");
                return guardRowExists("purge-30d");
            })
            .onSuccess(oldKept -> {
                context.assertFalse(oldKept, "A thirty day old row can no longer block anything and must go");
                async.complete();
            })
            .onFailure(context::fail);
    }

    /**
     * The purge loops until a batch deletes nothing, so running it on an already clean table must simply stop
     * rather than spin through its batch allowance.
     */
    @Test
    public void purgeShouldTerminateOnAnEmptyTable(TestContext context) {
        final Async async = context.async();
        raw("DELETE FROM conversation.absence_replies")
            .compose(v -> conversationService.purgeAbsenceReplies())
            .compose(v -> countGuardRows())
            .onSuccess(remaining -> {
                context.assertEquals(0, remaining, "Nothing to purge must leave nothing behind");
                async.complete();
            })
            .onFailure(context::fail);
    }
}
