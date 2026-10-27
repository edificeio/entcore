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

package org.entcore.conversation.service.impl;

import java.lang.reflect.Proxy;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.entcore.common.sql.Sql;
import org.entcore.common.storage.Storage;
import org.entcore.test.TestHelper;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.testcontainers.containers.PostgreSQLContainer;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Context;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;

/**
 * The orphan flag is set when the last link goes and never cleared, while a link can come back before the cron
 * runs : a thread restored from the trash, or replied to (IMPULS-6262). DeleteOrphan must keep whatever is linked
 * again, and must not delete the file of an attachment that is still linked.
 */
@RunWith(VertxUnitRunner.class)
public class DeleteOrphanRelinkTest {

    private static final TestHelper test = TestHelper.helper();
    static final String schema = "conversation";

    @ClassRule
    public static PostgreSQLContainer<?> pgContainer = test.database().createPostgreSQLContainer();

    private static DeleteOrphan deleteOrphan;
    private static Context deleteOrphanContext;
    private static final Set<String> removedFiles = Collections.synchronizedSet(new HashSet<>());

    @BeforeClass
    public static void setUp(TestContext context) throws Exception {
        // 027-fix-app-rights.sql grants default privileges for this role, which the test container does not have.
        pgContainer.execInContainer("psql", "-U", pgContainer.getUsername(), "-d", pgContainer.getDatabaseName(),
                "-c", "CREATE ROLE \"web-education\"");
        test.database().initPostgreSQL(context, pgContainer, schema);
        // DeleteOrphan reads its settings from the configuration of the verticle it is created in.
        final JsonObject config = new JsonObject().put("orphans-killer",
                new JsonObject().put("batch-size", 100).put("max-batches", 1));
        test.vertx().deployVerticle(new AbstractVerticle() {
            @Override
            public void start() {
                deleteOrphan = new DeleteOrphan(recordingStorage());
                deleteOrphanContext = vertx.getOrCreateContext();
            }
        }, new DeploymentOptions().setConfig(config), context.asyncAssertSuccess());
    }

    @SuppressWarnings("unchecked")
    private static Storage recordingStorage() {
        return (Storage) Proxy.newProxyInstance(Storage.class.getClassLoader(), new Class<?>[]{Storage.class},
            (proxy, method, args) -> {
                switch (method.getName()) {
                    case "removeFile":
                        removedFiles.add((String) args[0]);
                        ((Handler<JsonObject>) args[1]).handle(new JsonObject().put("status", "ok"));
                        return null;
                    case "toString":
                        return "recordingStorage";
                    default:
                        throw new UnsupportedOperationException(method.getName());
                }
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

    private Future<Void> runDeleteOrphan() {
        final Promise<Void> promise = Promise.promise();
        deleteOrphanContext.runOnContext(v -> deleteOrphan.deleteOrphanEntitiesInBatches().onComplete(promise));
        return promise.future();
    }

    @Test
    public void deleteOrphanShouldKeepWhatIsLinkedAgain(TestContext context) {
        final Async async = context.async();
        // Each pair : one flagged entity linked again, one flagged entity really orphan.
        raw("INSERT INTO conversation.threads (id, date, \"from\", \"to\", orphan) VALUES "
                + "('t-relinked', 1, 'u', '[]', true), ('t-orphan', 1, 'u', '[]', true)")
            .compose(v -> raw("INSERT INTO conversation.userthreads (user_id, thread_id) VALUES ('u1', 't-relinked')"))
            .compose(v -> raw("INSERT INTO conversation.messages (id, body, \"from\", \"to\", state, date, orphan) VALUES "
                + "('m-linked', 'b', 'u', '[]', 'SENT', 1, true), ('m-orphan', 'b', 'u', '[]', 'SENT', 1, true)"))
            .compose(v -> raw("INSERT INTO conversation.usermessages (user_id, message_id) VALUES ('u1', 'm-linked')"))
            .compose(v -> raw("INSERT INTO conversation.attachments (id, name, charset, filename, \"contentType\", "
                + "\"contentTransferEncoding\", size, orphan) VALUES "
                + "('a-linked', 'n', 'c', 'f', 't', 'e', 1, true), ('a-orphan', 'n', 'c', 'f', 't', 'e', 1, true)"))
            .compose(v -> raw("INSERT INTO conversation.usermessagesattachments (user_id, message_id, attachment_id) "
                + "VALUES ('u1', 'm-linked', 'a-linked')"))
            .compose(v -> runDeleteOrphan())
            .compose(v -> raw("SELECT id, orphan FROM conversation.threads ORDER BY id"))
            .compose(threads -> {
                context.assertEquals(new JsonArray().add(new JsonArray().add("t-relinked").add(false)), threads,
                    "The thread linked again must stay, unflagged ; the orphan one must go");
                return raw("SELECT count(*) FROM conversation.userthreads WHERE thread_id = 't-relinked'");
            })
            .compose(links -> {
                context.assertEquals(1, links.getJsonArray(0).getInteger(0), "The user must keep the thread in its list");
                return raw("SELECT id FROM conversation.messages ORDER BY id");
            })
            .compose(messages -> {
                context.assertEquals(new JsonArray().add(new JsonArray().add("m-linked")), messages);
                return raw("SELECT id FROM conversation.attachments ORDER BY id");
            })
            .onSuccess(attachments -> {
                context.assertEquals(new JsonArray().add(new JsonArray().add("a-linked")), attachments);
                context.assertEquals(Collections.singleton("a-orphan"), new HashSet<>(removedFiles),
                    "The file of an attachment still linked must never be deleted");
                async.complete();
            })
            .onFailure(context::fail);
    }
}
