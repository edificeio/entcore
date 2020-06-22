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

import org.entcore.common.folders.ElementQuery;
import org.entcore.common.folders.FolderManager;
import org.entcore.common.folders.QuotaService;
import org.entcore.common.notification.TimelineHelper;
import org.entcore.common.share.ShareService;
import org.entcore.common.storage.Storage;
import org.entcore.common.storage.StorageFactory;
import org.entcore.common.storage.impl.MongoDBApplicationStorage;
import org.entcore.common.user.UserInfos;
import org.entcore.common.utils.Config;
import org.entcore.test.TestHelper;
import org.entcore.workspace.dao.DocumentDao;
import org.entcore.workspace.service.WorkspaceService;
import org.entcore.workspace.service.impl.DefaultQuotaService;
import org.entcore.workspace.service.impl.DefaultWorkspaceService;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.containers.PostgreSQLContainer;

import fr.wseduc.mongodb.MongoDb;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;

@RunWith(VertxUnitRunner.class)
public class FolderTest {
    private static final TestHelper test = TestHelper.helper();
    @ClassRule
    public static MongoDBContainer mongoContainer = test.database().createMongoContainer();
    static WorkspaceService workspaceService;

    @BeforeClass
    public static void setUp(TestContext context) throws Exception {
        final ShareService shareService = test.share().createMongoShareService(context,
                DocumentDao.DOCUMENTS_COLLECTION);
        final Storage storage = new StorageFactory(test.vertx(), new JsonObject(),
                new MongoDBApplicationStorage(DocumentDao.DOCUMENTS_COLLECTION, Workspace.class.getSimpleName()))
                        .getStorage();
        final String imageResizerAddress = "wse.image.resizer";
        final FolderManager folderManager = FolderManager.mongoManager(DocumentDao.DOCUMENTS_COLLECTION, storage,
                test.vertx(), shareService, imageResizerAddress);
        final boolean neo4jPlugin = false;
        final QuotaService quotaService = new DefaultQuotaService(neo4jPlugin,
                new TimelineHelper(test.vertx(), test.vertx().eventBus(), new JsonObject()));
        final int threshold = 80;
        workspaceService = new DefaultWorkspaceService(storage, MongoDb.getInstance(), threshold, imageResizerAddress,
                quotaService, folderManager, test.vertx().eventBus(), shareService);
        test.database().initMongo(context, mongoContainer);
    }

    private JsonObject folder(String name) {
        return new JsonObject().put("name", name);
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

    private ElementQuery queryFolder() {
        return queryFolder(null);
    }

    private ElementQuery queryFolder(String parent) {
        final ElementQuery query = new ElementQuery(true);
        query.setType(FolderManager.FOLDER_TYPE);
        query.setParentId(parent);
        return query;
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

}
