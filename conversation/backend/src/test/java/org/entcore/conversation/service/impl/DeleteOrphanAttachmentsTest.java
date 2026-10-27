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
import java.nio.file.NoSuchFileException;

import org.entcore.common.sql.Sql;
import org.entcore.common.storage.FileStats;
import org.entcore.common.storage.Storage;
import org.entcore.test.TestHelper;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.testcontainers.containers.PostgreSQLContainer;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.AsyncResult;
import io.vertx.core.Context;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.core.file.FileSystemException;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;

/**
 * Purge of orphan attachments by DeleteOrphan (IMPULS-6262), on one batch of 5000 as configured on HDF.
 * The fake storage answers synchronously, like a storage whose deletions are already done when the results are
 * collected : chaining them one by one overflowed the stack. One file out of ten is already missing from the
 * storage, its row must go too ; one out of ten fails for another reason, its row must stay for a retry.
 */
@RunWith(VertxUnitRunner.class)
public class DeleteOrphanAttachmentsTest {

    private static final TestHelper test = TestHelper.helper();
    static final String schema = "conversation";
    private static final int BATCH = 5000;

    @ClassRule
    public static PostgreSQLContainer<?> pgContainer = test.database().createPostgreSQLContainer();

    private static DeleteOrphan deleteOrphan;
    private static Context deleteOrphanContext;

    @BeforeClass
    public static void setUp(TestContext context) throws Exception {
        // 027-fix-app-rights.sql grants default privileges for this role, which the test container does not have.
        pgContainer.execInContainer("psql", "-U", pgContainer.getUsername(), "-d", pgContainer.getDatabaseName(),
                "-c", "CREATE ROLE \"web-education\"");
        test.database().initPostgreSQL(context, pgContainer, schema);
        // DeleteOrphan reads its settings from the configuration of the verticle it is created in.
        final JsonObject config = new JsonObject().put("orphans-killer",
                new JsonObject().put("batch-size", BATCH).put("max-batches", 1));
        test.vertx().deployVerticle(new AbstractVerticle() {
            @Override
            public void start() {
                deleteOrphan = new DeleteOrphan(fakeStorage());
                deleteOrphanContext = vertx.getOrCreateContext();
            }
        }, new DeploymentOptions().setConfig(config), context.asyncAssertSuccess());
    }

    /** File index taken from the id "att-<n>" : n % 10 == 0 is already missing, n % 10 == 1 fails, the rest is deleted. */
    private static int index(String id) {
        return Integer.parseInt(id.substring("att-".length()));
    }

    @SuppressWarnings("unchecked")
    private static Storage fakeStorage() {
        return (Storage) Proxy.newProxyInstance(Storage.class.getClassLoader(), new Class<?>[]{Storage.class},
            (proxy, method, args) -> {
                switch (method.getName()) {
                    case "removeFile": {
                        final int n = index((String) args[0]);
                        final JsonObject res = n % 10 <= 1
                                ? new JsonObject().put("status", "error").put("message", "delete failed")
                                : new JsonObject().put("status", "ok");
                        ((Handler<JsonObject>) args[1]).handle(res);
                        return null;
                    }
                    case "fileStats": {
                        final int n = index((String) args[0]);
                        final AsyncResult<FileStats> res = n % 10 == 0
                                ? Future.failedFuture(new FileSystemException("Unable to analyse file",
                                        new NoSuchFileException("/storage/" + args[0])))
                                : Future.succeededFuture(new FileStats(0L, 0L, 1L));
                        ((Handler<AsyncResult<FileStats>>) args[1]).handle(res);
                        return null;
                    }
                    case "toString":
                        return "fakeStorage";
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

    @Test(timeout = 120000)
    public void batchOf5000ShouldDeleteRemovedAndMissingFiles(TestContext context) {
        final Async async = context.async();
        raw("INSERT INTO conversation.attachments (id, name, charset, filename, \"contentType\", \"contentTransferEncoding\", size, orphan) "
                + "SELECT 'att-' || n, 'n', 'c', 'f', 't', 'e', 1, true FROM generate_series(0, " + (BATCH - 1) + ") n")
            .compose(v -> runDeleteOrphan())
            .compose(v -> raw("SELECT count(*), count(*) FILTER (WHERE substr(id, 5)::int % 10 = 1) FROM conversation.attachments"))
            .onSuccess(rows -> {
                final JsonArray counts = rows.getJsonArray(0);
                context.assertEquals(BATCH / 10, counts.getInteger(0),
                    "Only the attachments whose file could not be deleted for another reason than its absence must remain");
                context.assertEquals(BATCH / 10, counts.getInteger(1));
                async.complete();
            })
            .onFailure(context::fail);
    }
}
