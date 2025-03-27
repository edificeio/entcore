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

package org.entcore.workspace;

import com.mongodb.client.model.Filters;
import fr.wseduc.mongodb.MongoDb;
import fr.wseduc.mongodb.MongoQueryBuilder;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.entcore.common.folders.ElementShareOperations;
import org.entcore.common.folders.FolderManager;
import org.entcore.common.folders.QuotaService;
import org.entcore.common.folders.impl.DocumentHelper;
import org.entcore.common.folders.impl.FolderImporterZip;
import org.entcore.common.notification.TimelineHelper;
import org.entcore.common.share.ShareService;
import org.entcore.common.storage.Storage;
import org.entcore.common.storage.StorageFactory;
import org.entcore.common.storage.impl.MongoDBApplicationStorage;
import org.entcore.common.user.UserInfos;
import org.entcore.test.TestHelper;
import org.entcore.workspace.controllers.WorkspaceController;
import org.entcore.workspace.dao.DocumentDao;
import org.entcore.workspace.service.WorkspaceService;
import org.entcore.workspace.service.impl.DefaultQuotaService;
import org.entcore.workspace.service.impl.DefaultWorkspaceService;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.containers.Neo4jContainer;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

@RunWith(VertxUnitRunner.class)
public class DocumentTest {
    static final int SIZE_3LEVELS = 2080215;
    private static final TestHelper test = TestHelper.helper();
    @ClassRule
    public static Neo4jContainer<?> neo4jContainer = test.database().createNeo4jContainer();
    @ClassRule
    public static MongoDBContainer mongoContainer = test.database().createMongoContainer();
    static WorkspaceService workspaceService;
    static String userid;

    @BeforeClass
    public static void setUp(TestContext context) throws Exception {
        test.database().initNeo4j(context, neo4jContainer);
        final ShareService shareService = test.share().createMongoShareService(context,
                DocumentDao.DOCUMENTS_COLLECTION);
        final Storage storage = new StorageFactory(test.vertx(), new JsonObject().put("file-system", new JsonObject().put("path", "/tmp")),
                new MongoDBApplicationStorage(DocumentDao.DOCUMENTS_COLLECTION, Workspace.class.getSimpleName()))
                .getStorage();
        final String imageResizerAddress = "wse.image.resizer";
        final FolderManager folderManager = FolderManager.mongoManager(DocumentDao.DOCUMENTS_COLLECTION, storage,
                test.vertx(), shareService, imageResizerAddress, false);
        final boolean neo4jPlugin = false;
        final QuotaService quotaService = new DefaultQuotaService(neo4jPlugin,
                new TimelineHelper(test.vertx(), test.vertx().eventBus(), new JsonObject()));
        final int threshold = 80;
        workspaceService = new DefaultWorkspaceService(storage, MongoDb.getInstance(), threshold, imageResizerAddress,
                quotaService, folderManager, test.vertx(), shareService, false);
        test.database().initMongo(context, mongoContainer);
        final Async async = context.async();
        test.directory().createActiveUser("user1", "password", "email").onComplete(res -> {
            context.assertTrue(res.succeeded());
            userid = res.result();
            async.complete();
        });
        final Async async2 = context.async();
        test.directory().createActiveUser(test.http().sessionUser()).onComplete(r -> {
            context.assertTrue(r.succeeded());
            async2.complete();
        });
    }

    private ElementShareOperations readWrite(UserInfos user) {
        return ElementShareOperations
                .addShareObject(WorkspaceController.SHARED_ACTION, user,
                        new JsonObject().put("groups", new JsonObject()).put("users",
                                new JsonObject().put(userid, new JsonArray()
                                        .add("org-entcore-workspace-controllers-WorkspaceController|updateDocument")
                                        .add("org-entcore-workspace-controllers-WorkspaceController|renameFolder"))));

    }

    private ElementShareOperations readOnly(UserInfos user) {
        return ElementShareOperations.addShareObject(WorkspaceController.SHARED_ACTION, user,
                new JsonObject().put("groups", new JsonObject()).put("users", new JsonObject().put(userid,
                        new JsonArray().add("org-entcore-workspace-controllers-WorkspaceController|renameFolder"))));
    }

    private JsonObject document(String name) {
        return new JsonObject().put("name", name);
    }

    private JsonObject folder(String name) {
        return new JsonObject().put("name", name);
    }

    private Future<String> createSharedFolder(TestContext context, String folder, UserInfos user) {
        Promise<String> future = Promise.promise();
        workspaceService.createFolder(folder(folder), user, res -> {
            context.assertTrue(res.succeeded());
            final String id = res.result().getString("_id");
            context.assertNotNull(id);
            workspaceService.share(id, readOnly(user), res1 -> {
                context.assertTrue(res1.succeeded());
                future.complete(id);
            });
        });
        return future.future();
    }

    private Future<String> createFolder(TestContext context, String folder, UserInfos user) {
      Promise<String> future = Promise.promise();
        workspaceService.createFolder(folder(folder), user, res -> {
            context.assertTrue(res.succeeded());
            final String id = res.result().getString("_id");
            future.complete(id);
        });
        return future.future();
    }

    @Test
    public void testWorkspaceServiceShouldCreateDocument(TestContext context) {
        final Async async = context.async();
        final UserInfos user = test.http().sessionUser();
        workspaceService.addFile(Optional.empty(), document("document1"), user.getUserId(), user.getUsername(),
                res1 -> {
                    context.assertTrue(res1.succeeded());
                    context.assertNotNull(res1.result().getString("_id"));
                    async.complete();
                });
    }

    @Test
    public void testWorkspaceServiceShouldShareDocument(TestContext context) {
        final Async async = context.async();
        final UserInfos user = test.http().sessionUser();
        workspaceService.addFile(Optional.empty(), document("document1"), user.getUserId(), user.getUsername(),
                res1 -> {
                    context.assertTrue(res1.succeeded());
                    final String id = res1.result().getString("_id");
                    final String modified = res1.result().getString("modified");
                    context.assertNotNull(id);
                    context.assertNotNull(modified);
                    workspaceService.share(id, readOnly(user), res3 -> {
                        context.assertTrue(res3.succeeded());
                        workspaceService.info(id, user, res4 -> {
                            context.assertTrue(res4.succeeded());
                            final String modified2 = res4.result().getString("modified");
                            context.assertEquals(modified, modified2);
                            async.complete();
                        });
                    });
                });
    }

    @Test
    public void testWorkspaceServiceShouldInheritShareDocument(TestContext context) {
        final Async async = context.async();
        final UserInfos user = test.http().sessionUser();
        createSharedFolder(context, "folder", user).onComplete(res0 -> {
            context.assertTrue(res0.succeeded());
            workspaceService.addFile(Optional.of(res0.result()), document("document1"), user.getUserId(),
                    user.getUsername(), res1 -> {
                        context.assertTrue(res1.succeeded());
                        final String id = res1.result().getString("_id");
                        final String modified = res1.result().getString("modified");
                        context.assertNotNull(id);
                        context.assertNotNull(modified);
                        final JsonArray shared = res1.result().getJsonArray("shared", new JsonArray());
                        final JsonArray inShared = res1.result().getJsonArray("inheritedShares");
                        context.assertEquals(0, shared.size());
                        context.assertEquals(1, inShared.size());
                        async.complete();
                    });
        });

    }

    @Test
    public void testWorkspaceServiceShouldInheritShareDocumentAndAugmentRights(TestContext context) {
        final Async async = context.async();
        final UserInfos user = test.http().sessionUser();
        final Set<String> userIds = new HashSet<>();
        userIds.add(user.getUserId());
        userIds.add(userid);
        test.vertx().eventBus().consumer("wse.communication.users").handler(message -> {
          String action = ((JsonObject)message.body()).getString("action", "");
          if("visibleUsers".equals(action)) {
            final JsonArray response = new JsonArray();
            for (String userId : userIds) {
              response.add(new JsonObject().put("id", userId));
            }
            message.reply(response);
          } else {
            message.fail(404, "not.found");
          }
        });
        createSharedFolder(context, "folder", user).onComplete(res0 -> {
            context.assertTrue(res0.succeeded());
            workspaceService.addFile(Optional.of(res0.result()), document("document1"), user.getUserId(),
                    user.getUsername(), res1 -> {
                        context.assertTrue(res1.succeeded());
                        final String id = res1.result().getString("_id");
                        final String modified = res1.result().getString("modified");
                        context.assertNotNull(id);
                        context.assertNotNull(modified);
                        workspaceService.share(id, readWrite(user), res2 -> {
                            context.assertTrue(res2.succeeded());
                            workspaceService.info(id, user, res3 -> {
                                context.assertTrue(res3.succeeded());
                                final String modified2 = res3.result().getString("modified");
                                final JsonArray shared = res3.result().getJsonArray("shared", new JsonArray());
                                final JsonArray inShared = res3.result().getJsonArray("inheritedShares");
                                context.assertEquals(1, shared.size());
                                context.assertEquals(2, inShared.size());
                                context.assertEquals(modified, modified2);
                                async.complete();
                            });
                        });
                    });
        });

    }

    @Test
    public void testWorkspaceServiceShouldShareChildOnly(TestContext context) {
        final Async async = context.async();
        final UserInfos user = test.http().sessionUser();
        createFolder(context, "folder", user).onComplete(res0 -> {
            context.assertTrue(res0.succeeded());
            workspaceService.addFile(Optional.of(res0.result()), document("document1"), user.getUserId(),
                    user.getUsername(), res1 -> {
                        context.assertTrue(res1.succeeded());
                        final String id = res1.result().getString("_id");
                        final String modified = res1.result().getString("modified");
                        context.assertNotNull(id);
                        context.assertNotNull(modified);
                        workspaceService.share(id, readWrite(user), res2 -> {
                            context.assertTrue(res2.succeeded());
                            workspaceService.info(id, user, res3 -> {
                                context.assertTrue(res3.succeeded());
                                final String modified2 = res3.result().getString("modified");
                                final JsonArray shared = res3.result().getJsonArray("shared", new JsonArray());
                                final JsonArray inShared = res3.result().getJsonArray("inheritedShares");
                                context.assertEquals(1, shared.size());
                                context.assertEquals(1, inShared.size());
                                context.assertEquals(modified, modified2);
                                async.complete();
                            });
                        });
                    });
        });

    }

    @Test
    public void testWorkspaceServiceShouldTrashDocument(TestContext context) {
        final Async async = context.async();
        final UserInfos user = test.http().sessionUser();
        workspaceService.addFile(Optional.empty(), document("document1"), user.getUserId(), user.getUsername(),
                res1 -> {
                    context.assertTrue(res1.succeeded());
                    final String id = res1.result().getString("_id");
                    final String modified = res1.result().getString("modified");
                    context.assertNotNull(id);
                    workspaceService.trash(id, user, res2 -> {
                        context.assertTrue(res2.succeeded());
                        test.database().executeMongoWithUniqueResult("documents",
                                MongoQueryBuilder.build(Filters.eq("_id", id))).onComplete(res3 -> {
                            context.assertTrue(res3.succeeded());
                            System.out.println(res3.result().encodePrettily());
                            Boolean deleted = res3.result().getBoolean("deleted");
                            final String modified2 = res3.result().getString("modified");
                            context.assertNotEquals(modified, modified2);
                            context.assertTrue(deleted);
                            async.complete();
                        });
                    });
                });
    }

    @Test
    public void testWorkspaceServiceShouldNotImportZipBecauseOfQuota(TestContext context) {
        final String zipPath = getClass().getResource("/zip-3-levels.zip").getPath();
        final Async async = context.async();
        final UserInfos user = test.http().sessionUser();
        test.userbook().setQuotaForUserId(user.getUserId(), (long) 0).onComplete(r -> {
            workspaceService.importFileZip(zipPath, user,
                    res1 -> {
                        context.assertTrue(res1.failed());
                        context.assertEquals("files.too.large", res1.cause().getMessage());
                        async.complete();
                    });
        });
    }

    @Test
    public void testWorkspaceServiceShouldImportZip(TestContext context) {
        final String zipPath = getClass().getResource("/zip-3-levels.zip").getPath();
        final Async async = context.async();
        final UserInfos user = test.http().sessionUser();
        test.userbook().setQuotaForUserId(user.getUserId(), (long) SIZE_3LEVELS).onComplete(r -> {
            context.assertTrue(r.succeeded());
            workspaceService.importFileZip(zipPath, user,
                    res1 -> {
                        context.assertTrue(res1.succeeded());
                        final JsonArray saved = res1.result().getJsonArray("saved");
                        final JsonArray errors = res1.result().getJsonArray("errors");
                        context.assertEquals(0, errors.size());
                        context.assertEquals(8, saved.size());
                        context.assertFalse(res1.result().containsKey("root"));
                        final long nbFiles = saved.stream().filter(e -> DocumentHelper.isFile((JsonObject) e)).count();
                        context.assertEquals(3l, nbFiles);
                        final Optional<JsonObject> file1 = saved.stream().map(e -> (JsonObject) e).filter(e -> "IMG_20200713_114421.jpg".equals(DocumentHelper.getName(e))).findFirst();
                        context.assertTrue(file1.isPresent());
                        context.assertEquals(4, DocumentHelper.getAncestors(file1.get()).size());
                        final Optional<JsonObject> file2 = saved.stream().map(e -> (JsonObject) e).filter(e -> "IMG-20200331-WA0000.jpg".equals(DocumentHelper.getName(e))).findFirst();
                        context.assertTrue(file2.isPresent());
                        context.assertEquals(3, DocumentHelper.getAncestors(file2.get()).size());
                        final Optional<JsonObject> dir1 = saved.stream().map(e -> (JsonObject) e).filter(e -> "test".equals(DocumentHelper.getName(e))).findFirst();
                        context.assertTrue(dir1.isPresent());
                        context.assertEquals(1, DocumentHelper.getAncestors(dir1.get()).size());
                        async.complete();
                    });
        });
    }
    @Test
    public void testWorkspaceServiceShouldImportZipInSharedFolder(TestContext context) {
        final String zipPath = getClass().getResource("/zip-3-levels.zip").getPath();
        final Async async = context.async();
        final UserInfos user = test.http().sessionUser();
        test.userbook().setQuotaForUserId(user.getUserId(), (long)2* SIZE_3LEVELS).onComplete(r -> {
            createSharedFolder(context, "folder", user).onComplete(res0 -> {
                context.assertTrue(res0.succeeded());
                workspaceService.info(res0.result(), user, res2 -> {
                    context.assertTrue(res2.succeeded());
                    final JsonObject root = res2.result();
                    final FolderImporterZip.FolderImporterZipContext ctx = new FolderImporterZip.FolderImporterZipContext(zipPath, null, user, "");
                    ctx.setRootFolder(root);
                    workspaceService.importFileZip(ctx,
                            res1 -> {
                                context.assertTrue(res1.succeeded());
                                final JsonArray saved = res1.result().getJsonArray("saved");
                                final JsonArray errors = res1.result().getJsonArray("errors");
                                context.assertEquals(0, errors.size());
                                context.assertEquals(8, saved.size());
                                final Optional<JsonObject> dir1 = saved.stream().map(e -> (JsonObject) e).filter(e -> "test".equals(DocumentHelper.getName(e))).findFirst();
                                context.assertTrue(dir1.isPresent());
                                context.assertEquals(1, dir1.get().getJsonArray("inheritedShares").size());
                                final Optional<JsonObject> file1 = saved.stream().map(e -> (JsonObject) e).filter(e -> "IMG_20200713_114421.jpg".equals(DocumentHelper.getName(e))).findFirst();
                                context.assertTrue(file1.isPresent());
                                context.assertEquals(1, file1.get().getJsonArray("inheritedShares").size());
                                async.complete();
                            });
                });
            });
        });
    }
    @Test
    public void testWorkspaceServiceShouldImportZipInFolder(TestContext context) {
        final String zipPath = getClass().getResource("/zip-3-levels.zip").getPath();
        final Async async = context.async();
        final UserInfos user = test.http().sessionUser();
        test.userbook().setQuotaForUserId(user.getUserId(), (long)3* SIZE_3LEVELS).onComplete(r -> {
            createFolder(context, "folder", user).onComplete(res0 -> {
                context.assertTrue(res0.succeeded());
                workspaceService.info(res0.result(), user, res2 -> {
                    context.assertTrue(res2.succeeded());
                    final JsonObject root = res2.result();
                    final FolderImporterZip.FolderImporterZipContext ctx = new FolderImporterZip.FolderImporterZipContext(zipPath, null, user, "");
                    ctx.setRootFolder(root);
                    workspaceService.importFileZip(ctx,
                            res1 -> {
                                context.assertTrue(res1.succeeded());
                                final JsonArray saved = res1.result().getJsonArray("saved");
                                final JsonArray errors = res1.result().getJsonArray("errors");
                                context.assertEquals(0, errors.size());
                                context.assertEquals(8, saved.size());
                                final Optional<JsonObject> file1 = saved.stream().map(e -> (JsonObject) e).filter(e -> "IMG_20200713_114421.jpg".equals(DocumentHelper.getName(e))).findFirst();
                                context.assertTrue(file1.isPresent());
                                context.assertEquals(5, DocumentHelper.getAncestors(file1.get()).size());
                                async.complete();
                            });
                });
            });
        });
    }


}
