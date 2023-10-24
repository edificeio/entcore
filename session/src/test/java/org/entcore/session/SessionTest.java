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

package org.entcore.session;

import java.util.HashMap;
import java.util.Map;

import org.entcore.test.TestHelper;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.containers.Neo4jContainer;

import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;

@RunWith(VertxUnitRunner.class)
public class SessionTest {
    private static final TestHelper test = TestHelper.helper();
    @ClassRule
    public static Neo4jContainer<?> neo4jContainer = test.database().createNeo4jContainer();
    @ClassRule
    public static MongoDBContainer mongoContainer = test.database().createMongoContainer();
    static AuthManager manager = new AuthManager();
    static Map<String, String> ids = new HashMap<>();

    @BeforeClass
    public static void setUp(TestContext context) throws Exception {
        test.initSharedData();
        test.database().initNeo4j(context, neo4jContainer);
        test.database().initMongo(context, mongoContainer);
        manager.init(test.vertx(), test.vertx().getOrCreateContext());
        manager.start();
        final Async async = context.async();
        test.directory().createActiveUser("login", "password", "email@test.com").compose(userId -> {
            ids.put("user1", userId);
            return test.directory().createStructure("Ecole1", "A111111").compose(structId -> {
                ids.put("struct1", structId);
                return test.directory().createProfileGroup("Enseignant").compose(profGroupId -> {
                    ids.put("group1", profGroupId);
                    return test.directory().attachUserToGroup(userId, profGroupId).compose(resAttach -> {
                        return test.directory().attachGroupToStruct(profGroupId, structId);
                    });
                });
            }).compose(res -> {
                return test.directory().createStructure("Ecole2", "B111111").compose(structId -> {
                    ids.put("struct2", structId);
                    return test.directory().createManualGroup("Groupe1").compose(groupId -> {
                        ids.put("group2", groupId);
                        return test.directory().attachUserToGroup(userId, groupId).compose(resAttach -> {
                            return test.directory().attachGroupToStruct(groupId, structId);
                        });
                    });
                });
            });
        }).compose(res -> {
            return test.directory().enableAppForStruct(ids.get("struct1"));
        }).onComplete(res -> {
            context.assertTrue(res.succeeded());
            async.complete();
        });

    }

    @Test
    public void testAuthManagerShouldGenerateSessionWithRightStructures(TestContext context) {
        final Async async = context.async();
        manager.generateSessionInfos(ids.get("user1"), res -> {
            context.assertEquals(1, res.getJsonArray("structures").size());
            context.assertEquals(1, res.getJsonArray("structureNames").size());
            context.assertEquals(1, res.getJsonArray("uai").size());
            context.assertTrue(res.getJsonArray("structures").contains(ids.get("struct1")));
            context.assertTrue(res.getJsonArray("structureNames").contains("Ecole1"));
            context.assertTrue(res.getJsonArray("uai").contains("A111111"));
            async.complete();
        });
    }

    @Test
    public void testAuthManagerShouldGenerateSessionWithRightGroups(TestContext context) {
        final Async async = context.async();
        manager.generateSessionInfos(ids.get("user1"), res -> {
            context.assertEquals(2, res.getJsonArray("groupsIds").size());
            context.assertTrue(res.getJsonArray("groupsIds").contains(ids.get("group1")));
            context.assertTrue(res.getJsonArray("groupsIds").contains(ids.get("group2")));
            async.complete();
        });
    }

    @Test
    public void testAuthManagerShouldGenerateSessionWithAppFlag(TestContext context) {
        final Async async = context.async();
        manager.generateSessionInfos(ids.get("user1"), res -> {
            context.assertTrue(res.getBoolean("hasApp"));
            async.complete();
        });
    }

}
