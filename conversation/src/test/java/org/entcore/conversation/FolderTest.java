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

package org.entcore.conversation;

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

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;

@RunWith(VertxUnitRunner.class)
public class FolderTest {
    private static final TestHelper test = TestHelper.helper();
    static final String schema = "conversation";
    @ClassRule
    public static PostgreSQLContainer<?> pgContainer = test.database().createPostgreSQLContainer();
    static ConversationService conversationService;

    @BeforeClass
    public static void setUp(TestContext context) throws Exception {
        Config.getInstance().setConfig(new JsonObject());
        conversationService = new SqlConversationService(test.vertx(), schema);
        test.database().initPostgreSQL(context, pgContainer, schema);
    }

    private JsonArray folderByName(JsonArray folders, String name) {
        final JsonArray res = new JsonArray();
        for (Object o : folders) {
            JsonObject json = (JsonObject) o;
            if (json.getString("name").equals(name)) {
                res.add(json);
            }
        }
        return res;
    }

    @Test
    public void testConversationServiceShouldCreateFolder(TestContext context) {
        final Async async = context.async();
        final UserInfos user = test.http().sessionUser();
        conversationService.createFolder("test", null, user, res1 -> {
            context.assertTrue(res1.isRight());
            async.complete();
        });
    }

    @Test
    public void testConversationServiceShouldNotCreateDuplicateFolder(TestContext context) {
        final Async async = context.async();
        final UserInfos user = test.http().sessionUser();
        conversationService.createFolder("duplicate1", null, user, res1 -> {
            context.assertTrue(res1.isRight());
            conversationService.createFolder("duplicate1", null, user, res2 -> {
                context.assertTrue(res2.isLeft());
                conversationService.listFolders(null, user, res3 -> {
                    context.assertTrue(res3.isRight());
                    context.assertEquals(1, folderByName(res3.right().getValue(), "duplicate1").size());
                    async.complete();
                });
            });
        });
    }

    @Test
    public void testConversationServiceShouldCreateChildFolder(TestContext context) {
        final Async async = context.async();
        final UserInfos user = test.http().sessionUser();
        conversationService.createFolder("test2", null, user, res1 -> {
            context.assertTrue(res1.isRight());
            final String id = res1.right().getValue().getString("id");
            context.assertNotNull(id, "Missing parent id");
            conversationService.createFolder("child2", id, user, res2 -> {
                context.assertTrue(res2.isRight());
                conversationService.listFolders(id, user, res3 -> {
                    context.assertTrue(res3.isRight());
                    context.assertEquals(1, folderByName(res3.right().getValue(), "child2").size());
                    async.complete();
                });
            });
        });
    }

    @Test
    public void testConversationServiceShouldNotCreateDuplicateChildFolder(TestContext context) {
        final Async async = context.async();
        final UserInfos user = test.http().sessionUser();
        conversationService.createFolder("test3", null, user, res1 -> {
            context.assertTrue(res1.isRight());
            final String id = res1.right().getValue().getString("id");
            context.assertNotNull(id, "Missing parent id");
            conversationService.createFolder("child3", id, user, res2 -> {
                context.assertTrue(res2.isRight());
                conversationService.createFolder("child3", id, user, res3 -> {
                    context.assertTrue(res3.isLeft());
                    context.assertEquals("conversation.error.duplicate.folder", res3.left().getValue());
                    conversationService.listFolders(id, user, res4 -> {
                        context.assertTrue(res4.isRight());
                        context.assertEquals(1, folderByName(res4.right().getValue(), "child3").size());
                        async.complete();
                    });
                });
            });
        });
    }

    @Test
    public void testConversationServiceShouldCreateFolderForDifferentUsers(TestContext context) {
        final Async async = context.async();
        final UserInfos user1 = test.http().sessionUser();
        final UserInfos user2 = test.directory().generateUser("a12345");
        conversationService.createFolder("test4", null, user1, res1 -> {
            context.assertTrue(res1.isRight());
            conversationService.createFolder("test4", null, user2, res2 -> {
                context.assertTrue(res2.isRight());
                conversationService.listFolders(null, user2, res3 -> {
                    context.assertTrue(res3.isRight());
                    context.assertEquals(1, folderByName(res3.right().getValue(), "test4").size());
                    async.complete();
                });
            });
        });
    }

    @Test
    public void testConversationServiceShouldCreateDuplicateFolderForOldData(TestContext context) {
        final Async async = context.async();
        final UserInfos user = test.http().sessionUser();
        conversationService.createFolder("duplicate5", null, user, res1 -> {
            context.assertTrue(res1.isRight());
            final String id = res1.right().getValue().getString("id");
            context.assertNotNull(id);
            test.database().executeSqlWithUniqueResult("UPDATE conversation.folders SET skip_uniq=NULL WHERE id = ?",
                    new JsonArray().add(id)).setHandler(resSql -> {
                        context.assertTrue(resSql.succeeded());
                        conversationService.createFolder("duplicate5", null, user, res2 -> {
                            context.assertTrue(res2.isRight());
                            conversationService.listFolders(null, user, res3 -> {
                                context.assertTrue(res3.isRight());
                                context.assertEquals(2, folderByName(res3.right().getValue(), "duplicate5").size());
                                async.complete();
                            });
                        });
                    });
        });
    }

    @Test
    public void testConversationServiceShouldCreateDuplicateChildFolderForOldData(TestContext context) {
        final Async async = context.async();
        final UserInfos user = test.http().sessionUser();
        conversationService.createFolder("duplicate6", null, user, res0 -> {
            context.assertTrue(res0.isRight());
            final String idParent = res0.right().getValue().getString("id");
            context.assertNotNull(idParent);
            conversationService.createFolder("duplicate6", idParent, user, res1 -> {
                context.assertTrue(res1.isRight());
                final String idChild = res1.right().getValue().getString("id");
                context.assertNotNull(idChild);
                test.database()
                        .executeSqlWithUniqueResult("UPDATE conversation.folders SET skip_uniq=NULL WHERE id = ?",
                                new JsonArray().add(idChild))
                        .setHandler(resSql -> {
                            context.assertTrue(resSql.succeeded());
                            conversationService.createFolder("duplicate6", idParent, user, res2 -> {
                                context.assertTrue(res2.isRight());
                                conversationService.listFolders(idParent, user, res3 -> {
                                    context.assertTrue(res3.isRight());
                                    context.assertEquals(2, folderByName(res3.right().getValue(), "duplicate6").size());
                                    async.complete();
                                });
                            });
                        });
            });
        });
    }

    @Test
    public void testConversationServiceShouldNotUpdateDuplicateFolderForOldData(TestContext context) {
        final Async async = context.async();
        final UserInfos user = test.http().sessionUser();
        conversationService.createFolder("duplicate7", null, user, res1 -> {
            context.assertTrue(res1.isRight());
            final String id = res1.right().getValue().getString("id");
            context.assertNotNull(id);
            test.database().executeSqlWithUniqueResult("UPDATE conversation.folders SET skip_uniq=NULL WHERE id = ?",
                    new JsonArray().add(id)).setHandler(resSql -> {
                        context.assertTrue(resSql.succeeded());
                        conversationService.createFolder("duplicate7", null, user, res2 -> {
                            context.assertTrue(res2.isRight());
                            final JsonObject data = new JsonObject().put("name", "duplicate7");
                            conversationService.updateFolder(id, data, user, res3 -> {
                                context.assertTrue(res3.isLeft());
                                context.assertEquals("conversation.error.duplicate.folder", res3.left().getValue());
                                async.complete();
                            });
                        });
                    });
        });
    }
}
