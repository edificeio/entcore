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

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import io.vertx.core.json.JsonArray;
import org.entcore.common.folders.ElementQuery;
import org.entcore.common.folders.ElementShareOperations;
import org.entcore.common.folders.FolderManager;
import org.entcore.common.folders.QuotaService;
import org.entcore.common.folders.impl.DocumentHelper;
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
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.testcontainers.containers.MongoDBContainer;

import fr.wseduc.mongodb.MongoDb;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.testcontainers.containers.Neo4jContainer;

@RunWith(VertxUnitRunner.class)
public class FolderTest {
    private static final TestHelper test = TestHelper.helper();
    @ClassRule
    public static Neo4jContainer<?> neo4jContainer = test.database().createNeo4jContainer();
    @ClassRule
    public static MongoDBContainer mongoContainer = test.database().createMongoContainer();
    static WorkspaceService workspaceService;

    @BeforeClass
    public static void setUp(TestContext context) throws Exception {
        test.database().initNeo4j(context, neo4jContainer);
        final ShareService shareService = test.share().createMongoShareService(context,
                DocumentDao.DOCUMENTS_COLLECTION);
        StorageFactory.build(test.vertx(), new JsonObject().put("file-system", new JsonObject().put("path", "/tmp")),
                new MongoDBApplicationStorage(DocumentDao.DOCUMENTS_COLLECTION, Workspace.class.getSimpleName()))
            .onSuccess(storageFactory -> {
                final Storage storage = storageFactory.getStorage();
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
            }).onFailure(ex -> context.fail(ex));
    }

    @Before
    public void beforeTest() {
        workspaceService.setAllowDuplicate(false);
    }

    private JsonObject folder(String name) {
        return new JsonObject().put("name", name);
    }

    private JsonObject folder(String name, final String id) {
        return new JsonObject().put("name", name).put("_id", id);
    }

    private ElementShareOperations readOnly(UserInfos user, final String userid) {
        return ElementShareOperations.addShareObject(WorkspaceController.SHARED_ACTION, user,
                new JsonObject().put("groups", new JsonObject()).put("users", new JsonObject().put(userid,
                        new JsonArray().add("org-entcore-workspace-controllers-WorkspaceController|renameFolder"))));
    }

    private ElementShareOperations readWrite(UserInfos user, final String userid) {
        return ElementShareOperations.addShareObject(WorkspaceController.SHARED_ACTION, user,
                new JsonObject().put("groups", new JsonObject()).put("users", new JsonObject().put(userid,
                        new JsonArray().add("org-entcore-workspace-controllers-WorkspaceController|renameFolder")
                                .add("org-entcore-workspace-controllers-WorkspaceController|updateDocument"))));
    }

    @Test
    public void testWorkspaceServiceShouldCreateFolder(TestContext context) {
        final Async async = context.async();
        final UserInfos user = test.http().sessionUser();
        workspaceService.createFolder(folder("test1"), user, res1 -> {
            context.assertTrue(res1.succeeded());
            context.assertNotNull(res1.result().getString("_id"));
            async.complete();
        });
    }

    @Test
    public void testWorkspaceServiceShouldNotCreateDuplicateFolder(TestContext context) {
        final Async async = context.async();
        final UserInfos user = test.http().sessionUser();
        workspaceService.createFolder("test1", user, folder("test2"), res0 -> {
            context.assertTrue(res0.succeeded());
            workspaceService.createFolder("test1", user, folder("test2"), res1 -> {
                context.assertTrue(res1.failed());
                context.assertEquals("folders.errors.duplicate.folder", res1.cause().getMessage());
                async.complete();
            });
        });
    }

    @Test
    public void testWorkspaceServiceShouldCreateDuplicateFolder(TestContext context) {
        workspaceService.setAllowDuplicate(true);
        final Async async = context.async();
        final UserInfos user = test.http().sessionUser();
        workspaceService.createFolder("test1", user, folder("test2-c"), res0 -> {
            context.assertTrue(res0.succeeded());
            workspaceService.createFolder("test1", user, folder("test2-c"), res1 -> {
                context.assertTrue(res1.succeeded());
                async.complete();
            });
        });
    }

    @Test
    public void testWorkspaceServiceShouldCreateDuplicateFolderAtRoot(TestContext context) {
        final Async async = context.async();
        final UserInfos user = test.http().sessionUser();
        workspaceService.createFolder(folder("test2-a"), user, res0 -> {
            context.assertTrue(res0.succeeded());
            workspaceService.createFolder(folder("test2-a"), user, res1 -> {
                context.assertTrue(res1.succeeded());
                async.complete();
            });
        });
    }

    @Test
    public void testWorkspaceServiceShouldNotRenameDuplicateFolder(TestContext context) {
        final Async async = context.async();
        final UserInfos user = test.http().sessionUser();
        workspaceService.createFolder("test1", user, folder("test3"), res0 -> {
            context.assertTrue(res0.succeeded());
            workspaceService.createFolder("test1", user, folder("test3-b"), res1 -> {
                context.assertTrue(res1.succeeded());
                final String id = res1.result().getString("_id");
                context.assertNotNull(id);
                workspaceService.rename(id, "test3", user, res2 -> {
                    context.assertTrue(res2.failed());
                    context.assertEquals("folders.errors.duplicate", res2.cause().getMessage());
                    async.complete();
                });
            });
        });
    }

    @Test
    public void testWorkspaceServiceShouldRenameDuplicateFolder(TestContext context) {
        workspaceService.setAllowDuplicate(true);
        final Async async = context.async();
        final UserInfos user = test.http().sessionUser();
        workspaceService.createFolder("test1", user, folder("test3-c"), res0 -> {
            context.assertTrue(res0.succeeded());
            workspaceService.createFolder("test1", user, folder("test3-d"), res1 -> {
                context.assertTrue(res1.succeeded());
                final String id = res1.result().getString("_id");
                context.assertNotNull(id);
                workspaceService.rename(id, "test3-c", user, res2 -> {
                    context.assertTrue(res2.succeeded());
                    async.complete();
                });
            });
        });
    }

    @Test
    public void testWorkspaceServiceShouldCreateDocument(TestContext context) {
        final Async async = context.async();
        final UserInfos user = test.http().sessionUser();
        workspaceService.addFile(Optional.empty(), folder("file1"), user.getUserId(), user.getUsername(), res1 -> {
            context.assertTrue(res1.succeeded());
            context.assertNotNull(res1.result().getString("_id"));
            async.complete();
        });
    }

    @Test
    public void testWorkspaceServiceShouldNotCreateDuplicateDocument(TestContext context) {
        final Async async = context.async();
        final UserInfos user = test.http().sessionUser();
        workspaceService.addFile(Optional.of("test1"), folder("file2"), user.getUserId(), user.getUsername(), res0 -> {
            context.assertTrue(res0.succeeded());
            workspaceService.addFile(Optional.of("test1"), folder("file2"), user.getUserId(), user.getUsername(),
                    res1 -> {
                        context.assertTrue(res1.succeeded());
                        context.assertEquals("file2_1", res1.result().getString("name"));
                        async.complete();
                    });
        });
    }

    @Test
    public void testWorkspaceServiceShouldCreateDuplicateDocument(TestContext context) {
        workspaceService.setAllowDuplicate(true);
        final Async async = context.async();
        final UserInfos user = test.http().sessionUser();
        workspaceService.addFile(Optional.of("test1"), folder("file2-b"), user.getUserId(), user.getUsername(),
                res0 -> {
                    context.assertTrue(res0.succeeded());
                    workspaceService.addFile(Optional.of("test1"), folder("file2-b"), user.getUserId(),
                            user.getUsername(), res1 -> {
                                context.assertTrue(res1.succeeded());
                                context.assertEquals("file2-b", res1.result().getString("name"));
                                async.complete();
                            });
                });
    }

    @Test
    public void testWorkspaceServiceShouldNotCreateDuplicateDocumentWhen1Match(TestContext context) {
        final Async async = context.async();
        final UserInfos user = test.http().sessionUser();
        workspaceService.addFile(Optional.of("test1"), folder("file10"), user.getUserId(), user.getUsername(), res0 -> {
            context.assertTrue(res0.succeeded());
            workspaceService.addFile(Optional.of("test1"), folder("file10_1"), user.getUserId(), user.getUsername(),
                    res1 -> {
                        workspaceService.addFile(Optional.of("test1"), folder("file10"), user.getUserId(),
                                user.getUsername(), res2 -> {
                                    context.assertTrue(res2.succeeded());
                                    context.assertEquals("file10_2", res2.result().getString("name"));
                                    async.complete();
                                });
                    });
        });
    }

    @Test
    public void testWorkspaceServiceShouldCreateDuplicateDocumentAtRoot(TestContext context) {
        final Async async = context.async();
        final UserInfos user = test.http().sessionUser();
        workspaceService.addFile(Optional.empty(), folder("file2-c"), user.getUserId(), user.getUsername(), res0 -> {
            context.assertTrue(res0.succeeded());
            workspaceService.addFile(Optional.empty(), folder("file2-c"), user.getUserId(), user.getUsername(),
                    res1 -> {
                        context.assertTrue(res1.succeeded());
                        async.complete();
                    });
        });
    }

    @Test
    public void testWorkspaceServiceShouldRenameSelf(TestContext context) {
        final Async async = context.async();
        final UserInfos user = test.http().sessionUser();
        workspaceService.createFolder("test1", user, folder("file4"), res0 -> {
            context.assertTrue(res0.succeeded());
            final String id = res0.result().getString("_id");
            context.assertNotNull(id);
            workspaceService.rename(id, "file4", user, res2 -> {
                context.assertTrue(res2.succeeded());
                async.complete();
            });
        });
    }

    @Test
    public void testWorkspaceServiceShouldNotCreateDuplicateDocumentWithExt(TestContext context) {
        final Async async = context.async();
        final UserInfos user = test.http().sessionUser();
        workspaceService.addFile(Optional.of("test1"), folder("file5.a.png"), user.getUserId(), user.getUsername(), res0 -> {
            context.assertTrue(res0.succeeded());
            workspaceService.addFile(Optional.of("test1"), folder("file5.a.png"), user.getUserId(), user.getUsername(),
                    res1 -> {
                        context.assertTrue(res1.succeeded());
                        context.assertEquals("file5.a_1.png", res1.result().getString("name"));
                        async.complete();
                    });
        });
    }
    /**
     * <u>GOAL</u> : Reset shared when restoring a child
     *
     * <u>STEPS</u> :
     * <ol>
     *     <li>Create user "reader" and "writer"</li>
     *     <li>Create folder1 for user13</li>
     *     <li>Create folder2 for user13 (child of folder1)</li>
     *     <li>Verify that folder1 is child of folder2</li>
     *     <li>Share folder1 to reader</li>
     *     <li>Share folder2 to writer</li>
     *     <li>Verify that folder1 has share to reader</li>
     *     <li>Verify that folder2 has shared share to writer</li>
     *     <li>Verify that folder2 has inherited share to reader and writer</li>
     *     <li>Delete folder1</li>
     *     <li>Verify that folder1 and folder2 has been deleted</li>
     *     <li>Restore folder2</li>
     *     <li>Verify that folder2 has no more parent and is not trashed</li>
     *     <li>Verify that folder2 has direct shared to reader and writer</li>
     *     <li>Verify that folder2 has inherited shared to reader</li>
     * </ol>
     *
     * @param context
     * @throws Exception
     */
    @Test
    public void shouldRestoreAChild(TestContext context) {
        final Async async = context.async();
        final UserInfos user = test.directory().generateUser("user13");
        final Set<String> userIds = new HashSet<>();
        userIds.add(user.getUserId());
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
        // create reader
        test.directory().createActiveUser("reader", "password", "email").onComplete(context.asyncAssertSuccess(reader -> {
            userIds.add(reader);
            // create writer
            test.directory().createActiveUser("writter", "password", "email").onComplete(context.asyncAssertSuccess(writter -> {
                userIds.add(writter);
                // create folder tree
                workspaceService.createFolder(folder("folder1", "folder1"), user, context.asyncAssertSuccess(onCreateFolder1 -> {
                    workspaceService.createFolder("folder1", user, folder("folder2", "folder2"), context.asyncAssertSuccess(onCreateFolder2 -> {
                        // share folder tree
                        workspaceService.share("folder1", readOnly(user, reader), context.asyncAssertSuccess(onShare -> {
                            workspaceService.share("folder2", readWrite(user, writter), context.asyncAssertSuccess(onShareWriter -> {
                                workspaceService.findByQuery(new ElementQuery(true), user, context.asyncAssertSuccess(folders -> {
                                    // check folder tree
                                    context.assertEquals(2, folders.size());
                                    final JsonObject folder1 = folders.getJsonObject(0);
                                    final JsonObject folder2 = folders.getJsonObject(1);
                                    context.assertEquals("folder1", DocumentHelper.getName(folder1));
                                    context.assertEquals("folder2", DocumentHelper.getName(folder2));
                                    context.assertNull(DocumentHelper.getParent(folder1));
                                    context.assertEquals("folder1", DocumentHelper.getParent(folder2));
                                    context.assertEquals(1, folder1.getJsonArray("shared").size());
                                    context.assertEquals(1, folder1.getJsonArray("inheritedShares").size());
                                    context.assertEquals(1, folder2.getJsonArray("shared").size());
                                    context.assertEquals(2, folder2.getJsonArray("inheritedShares").size());
                                    // trash folder tree
                                    workspaceService.trash("folder1", user, context.asyncAssertSuccess(onDelete -> {
                                        final ElementQuery queryTrashed = new ElementQuery(true);
                                        queryTrashed.setTrash(true);
                                        queryTrashed.setHierarchical(true);
                                        workspaceService.findByQuery(queryTrashed, user, context.asyncAssertSuccess(deletedFolders -> {
                                            // check trash
                                            context.assertEquals(2, deletedFolders.size());
                                            final JsonObject delFolder1 = deletedFolders.getJsonObject(0);
                                            final JsonObject delFolder2 = deletedFolders.getJsonObject(1);
                                            context.assertEquals(true, delFolder1.getBoolean("deleted"));
                                            context.assertEquals(1, delFolder1.getJsonArray("shared").size());
                                            context.assertEquals(1, delFolder1.getJsonArray("inheritedShares").size());
                                            context.assertEquals(true, delFolder2.getBoolean("deleted"));
                                            context.assertEquals(1, delFolder2.getJsonArray("shared").size());
                                            context.assertEquals(2, delFolder2.getJsonArray("inheritedShares").size());
                                            // restore folder2
                                            workspaceService.restore("folder2", user, context.asyncAssertSuccess(onRestore -> {
                                                final ElementQuery queryRestored = new ElementQuery(true);
                                                queryRestored.setTrash(false);
                                                queryRestored.setHierarchical(true);
                                                workspaceService.findByQuery(queryRestored, user, context.asyncAssertSuccess(restoredFolders -> {
                                                    // check restore
                                                    context.assertEquals(1, restoredFolders.size());
                                                    final JsonObject restoredFolder1 = restoredFolders.getJsonObject(0);
                                                    context.assertNull(DocumentHelper.getParent(restoredFolder1));
                                                    context.assertEquals(false, restoredFolder1.getBoolean("deleted"));
                                                    // should have direct shares to reader1
                                                    context.assertEquals(2, restoredFolder1.getJsonArray("shared").size());
                                                    context.assertEquals(2, restoredFolder1.getJsonArray("inheritedShares").size());
                                                    async.complete();
                                                }));
                                            }));
                                        }));
                                    }));
                                }));
                            }));
                        }));
                    }));
                }));
            }));
        }));
    }
}
