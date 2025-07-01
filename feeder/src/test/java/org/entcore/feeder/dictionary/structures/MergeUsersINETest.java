package org.entcore.feeder.dictionary.structures;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.entcore.common.events.EventStoreFactory;
import org.entcore.common.neo4j.Neo4j;
import org.entcore.common.neo4j.TransactionHelper;
import org.entcore.feeder.exceptions.TransactionException;
import org.entcore.feeder.utils.TransactionManager;
import org.entcore.feeder.utils.Validator;
import org.entcore.test.TestHelper;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.testcontainers.containers.Neo4jContainer;

@RunWith(VertxUnitRunner.class)
public class MergeUsersINETest {
    private static final TestHelper test = TestHelper.helper();
    @ClassRule
    public static Neo4jContainer<?> neo4jContainer = test.database().createNeo4jContainer();

    private static DuplicateUsers duplicateUsers;
    private static Neo4j neo4j;

    @BeforeClass
    public static void setUp(TestContext context) throws Exception {
        EventStoreFactory.getFactory().setVertx(test.vertx());
        duplicateUsers = new DuplicateUsers(false, false, test.vertx().eventBus());
        test.database().initNeo4j(context, neo4jContainer);
        final String base = neo4jContainer.getHttpUrl() + "/db/data/";
        final JsonObject neo4jConfig = new JsonObject()
                .put("server-uri", base).put("poolSize", 1);
        neo4j = Neo4j.getInstance();
        neo4j.init(test.vertx(), neo4jConfig
                .put("server-uri", base)
                .put("ignore-empty-statements-error", false));
        Validator.initLogin(neo4j, test.vertx());
        TransactionManager.getInstance().setNeo4j(neo4j);
    }

    /**
     * <h2>Goal</h2>
     * <p>Test that when we merge 2 users with the same INE and each with a userbook, we don't end up with a user linked to 2 userbooks.</p>
     */
    @Test
    public void testMergeSameINEWithEachAUserBookNoDoubleUserBook(final TestContext testContext) {
        final String ine = "my-duplicated-ine";
        final Async async = testContext.async(2);
        prepareSameINEUsersWithAUserBookEach(ine).onComplete(testContext.asyncAssertSuccess(h -> {
            duplicateUsers.mergeSameINE(true, testContext.asyncAssertSuccess(e -> {
                neo4j.execute(
                    "MATCH (u:User{ine:{ine}, tag: 'unitTest'})-[r:USERBOOK]->(ub:UserBook) return u.id as id, u.ine as ub_ine, ub.userid as ub_user_id, collect(ub) as ubs ",
                    new JsonObject().put("ine", ine),
                    result -> {
                        if ("ok".equals(result.body().getString("status"))) {
                            final JsonArray users = result.body().getJsonArray("result");
                            testContext.assertEquals(1, users.size(), "There should be only one user left");
                            final JsonObject principalUser = users.getJsonObject(0);
                            testContext.assertEquals("userToRemove", principalUser.getString("id"), "The remaining user is not the expected one. Have source priorities changed ?");
                            testContext.assertEquals(ine, principalUser.getString("ub_ine"), "The ine is not the expected one. Has INE handling changed ?");
                            testContext.assertEquals("userToKeep", principalUser.getString("ub_user_id"), "The connected userbook is not the one that we should have. Has anything changed regarding the userbook policy ?");
                            async.countDown();
                        } else {
                            testContext.fail("Could not fetch users with the same ine");
                        }
                    });
                neo4j.execute(
                        "MATCH (ub:UserBook{tag: 'unitTest'}) return ub.userid as user_id", new JsonObject(),
                        result -> {
                            if ("ok".equals(result.body().getString("status"))) {
                                final JsonArray userbooks = result.body().getJsonArray("result");
                                testContext.assertEquals(1, userbooks.size(), "There should be only one userbook left");
                                final JsonObject ub = userbooks.getJsonObject(0);
                                testContext.assertEquals("userToKeep", ub.getString("user_id"), "The remaining user is not the expected one. Have source priorities changed ?");
                                async.complete();
                            } else {
                                testContext.fail("Could not fetch users with the same ine");
                            }
                        });
            }));
        }));
    }
    /**
     * <h2>Goal</h2>
     * <p>Test that when we merge 2 users with the same INE and only one user book linked to the user to be removed,
     * we don't end up with a user linked to a userbook whose userid is not his/her.</p>
     */
    @Test
    public void testMergeSameINEWithOnlyOneUserBook(final TestContext testContext) {
        final String ine = "my-duplicated-ine-2";
        final Async async = testContext.async();
        prepareSameINEUsersWithOnlyOneUserBook(ine).onComplete(testContext.asyncAssertSuccess(h -> {
            duplicateUsers.mergeSameINE(true, testContext.asyncAssertSuccess(e -> {
                neo4j.execute(
                        "MATCH (u:User{ine:{ine}, tag: 'unitTest2'})-[r:USERBOOK]->(ub:UserBook) return u.id as id, u.ine as ub_ine, ub.userid as ub_user_id",
                        new JsonObject().put("ine", ine),
                        result -> {
                            if ("ok".equals(result.body().getString("status"))) {
                                final JsonArray users = result.body().getJsonArray("result");
                                testContext.assertEquals(1, users.size(), "There should be only one user left");
                                final JsonObject principalUser = users.getJsonObject(0);
                                testContext.assertEquals("userToKeep1Userbook", principalUser.getString("ub_user_id"), "The connected userbook's userid is not the one that we should have. Has anything changed regarding the userbook policy ?");
                                async.complete();
                            } else {
                                testContext.fail("Could not fetch users with the same ine");
                            }
                        });
            }));
        }));
    }

    /**
     * Creates 2 users as follows :
     * <ol>
     *     <li>(:User{id: 'userToKeep', source: 'AAF', ine: ine, activationCode: 'toto'})-[:USERBOOK]->(:UserBook{tag: 'unitTest', userid: 'userToKeep'})</li>
     *     <li>(:User{id: 'userToRemove', source: 'MANUAL', ine: ine})-[:USERBOOK]->(:UserBook{tag: 'unitTest', userid: 'userToRemove'})</li>
     * </ol>
     * @param ine The ine to use for both users
     */
    public static Future<Void> prepareSameINEUsersWithAUserBookEach(final String ine) {
        final JsonObject params = new JsonObject().put("ine", ine);
        final Promise<Void> promise = Promise.promise();
        TransactionHelper txl = null;
        try {
            txl = TransactionManager.getTransaction();
            txl.add("match (u) detach delete u", new JsonObject());
            txl.add("create (u1:User{id: 'userToKeep', source: 'AAF', activationCode: 'toto', ine: {ine}, tag: 'unitTest', activated:true})-[:USERBOOK]->(:UserBook{tag: 'unitTest', userid: 'userToKeep'})", params);
            txl.add("create (u2:User{id: 'userToRemove', source: 'MANUAL', ine: {ine}, tag: 'unitTest'})-[:USERBOOK]->(:UserBook{tag: 'unitTest2', userid: 'userToRemove'})", params);
            txl.commit(event -> {
                if ("ok".equals(event.body().getString("status"))) {
                    promise.complete();
                } else {
                    promise.fail(event.body().encodePrettily());
                }
            });
        } catch (TransactionException e) {
            promise.fail(e);
        }
        return promise.future();
    }

    /**
     * Creates 2 users as follows :
     * <ol>
     *     <li>(:User{id: 'userToKeep', source: 'AAF', ine: ine, activationCode: 'toto'})</li>
     *     <li>(:User{id: 'userToRemove', source: 'MANUAL', ine: ine})-[:USERBOOK]->(:UserBook{tag: 'unitTest', userid: 'userToRemove'})</li>
     * </ol>
     * @param ine The ine to use for both users
     */
    public static Future<Void> prepareSameINEUsersWithOnlyOneUserBook(final String ine) {
        final JsonObject params = new JsonObject().put("ine", ine);
        final Promise<Void> promise = Promise.promise();
        TransactionHelper txl = null;
        try {
            txl = TransactionManager.getTransaction();
            txl.add("match (u) detach delete u", new JsonObject());
            txl.add("create (u1:User{id: 'userToKeep1Userbook', source: 'AAF', activationCode: 'toto', ine: {ine}, tag: 'unitTest2'})", params);
            txl.add("create (u2:User{id: 'userToRemove1Userbook', source: 'MANUAL', ine: {ine}, tag: 'unitTest2'})-[:USERBOOK]->(:UserBook{tag: 'unitTest', userid: 'userToRemove1Userbook'})", params);
            txl.commit(event -> {
                if ("ok".equals(event.body().getString("status"))) {
                    promise.complete();
                } else {
                    promise.fail(event.body().encodePrettily());
                }
            });
        } catch (TransactionException e) {
            promise.fail(e);
        }
        return promise.future();
    }

}
