package org.entcore.feeder.dictionary.structures;

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

import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;

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
     * we don't end up with a user linked to a userbook whose userid is not his/her own (post-merge) id. Since
     * COCO-4598, the surviving user keeps the old (removed) user's id, and the transferred userbook's userid is set
     * to that same old id, so the two must match.</p>
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
                                testContext.assertEquals("userToRemove1Userbook", principalUser.getString("id"), "The remaining user is not the expected one. Have source priorities changed ?");
                                testContext.assertEquals(principalUser.getString("id"), principalUser.getString("ub_user_id"), "The connected userbook's userid is not the one that we should have. Has anything changed regarding the userbook policy ?");
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
     * <p>Test that when we merge 2 users with the same INE and each with a UserAppConf (PREFERS), we don't end up
     * with a user linked to 2 UserAppConf, nor with the old one left as an orphan node.</p>
     */
    @Test
    public void testMergeSameINEWithEachAUserAppConfNoDuplicatePreferences(final TestContext testContext) {
        final String ine = "my-duplicated-ine-3";
        final Async async = testContext.async(2);
        prepareSameINEUsersWithAUserAppConfEach(ine).onComplete(testContext.asyncAssertSuccess(h -> {
            duplicateUsers.mergeSameINE(true, testContext.asyncAssertSuccess(e -> {
                neo4j.execute(
                    "MATCH (u:User{ine:{ine}, tag: 'unitTest3'})-[r:PREFERS]->(uac:UserAppConf) return u.id as id, uac.owner as uac_owner",
                    new JsonObject().put("ine", ine),
                    result -> {
                        if ("ok".equals(result.body().getString("status"))) {
                            final JsonArray users = result.body().getJsonArray("result");
                            testContext.assertEquals(1, users.size(), "There should be only one user left, linked to exactly one UserAppConf");
                            final JsonObject principalUser = users.getJsonObject(0);
                            testContext.assertEquals("userToRemove3", principalUser.getString("id"), "The remaining user is not the expected one. Have source priorities changed ?");
                            testContext.assertEquals("userToKeep3", principalUser.getString("uac_owner"), "The connected UserAppConf should be the principal's own one, not the old duplicate");
                            async.countDown();
                        } else {
                            testContext.fail("Could not fetch users with the same ine");
                        }
                    });
                neo4j.execute(
                        "MATCH (uac:UserAppConf) WHERE uac.tag STARTS WITH 'unitTest3' return uac.owner as owner",
                        new JsonObject(),
                        result -> {
                            if ("ok".equals(result.body().getString("status"))) {
                                final JsonArray uacs = result.body().getJsonArray("result");
                                testContext.assertEquals(1, uacs.size(), "There should be only one UserAppConf left, the old duplicate must be deleted, not left as an orphan");
                                testContext.assertEquals("userToKeep3", uacs.getJsonObject(0).getString("owner"), "The remaining UserAppConf is not the expected one");
                                async.complete();
                            } else {
                                testContext.fail("Could not fetch UserAppConf nodes");
                            }
                        });
            }));
        }));
    }

    /**
     * <h2>Goal</h2>
     * <p>Test that when we merge 2 users with the same INE and only one has a UserAppConf linked, the preference is
     * correctly transferred to the surviving user.</p>
     */
    @Test
    public void testMergeSameINEWithOnlyOneUserAppConf(final TestContext testContext) {
        final String ine = "my-duplicated-ine-4";
        final Async async = testContext.async();
        prepareSameINEUsersWithOnlyOneUserAppConf(ine).onComplete(testContext.asyncAssertSuccess(h -> {
            duplicateUsers.mergeSameINE(true, testContext.asyncAssertSuccess(e -> {
                neo4j.execute(
                        "MATCH (u:User{ine:{ine}, tag: 'unitTest4'})-[r:PREFERS]->(uac:UserAppConf) return u.id as id, uac.owner as uac_owner",
                        new JsonObject().put("ine", ine),
                        result -> {
                            if ("ok".equals(result.body().getString("status"))) {
                                final JsonArray users = result.body().getJsonArray("result");
                                testContext.assertEquals(1, users.size(), "There should be only one user left, linked to the transferred UserAppConf");
                                final JsonObject principalUser = users.getJsonObject(0);
                                testContext.assertEquals("userToRemove4", principalUser.getString("uac_owner"), "The transferred UserAppConf's owner is not the expected one");
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
    * <p>Test that when the principal user already has duplicate UserAppConf nodes, the merge keeps the node carrying
    * language/timeline and recovers one quietHours value from the discarded duplicates.</p>
     */
    @Test
    public void testMergeSameINEMergesAlreadyDuplicatedPreferences(final TestContext testContext) {
        final String ine = "my-duplicated-ine-5";
        final Async async = testContext.async(2);
        prepareSameINEUsersWithAlreadyDuplicatedPreferences(ine).onComplete(testContext.asyncAssertSuccess(h -> {
            duplicateUsers.mergeSameINE(true, testContext.asyncAssertSuccess(e -> {
                neo4j.execute(
                    "MATCH (u:User{ine:{ine}, tag: 'unitTest5'})-[r:PREFERS]->(uac:UserAppConf) return u.id as id, uac.owner as owner, uac.language as language, uac.timeline as timeline, uac.quietHours as quietHours, uac.timezone as timezone",
                    new JsonObject().put("ine", ine),
                    result -> {
                        if ("ok".equals(result.body().getString("status"))) {
                            final JsonArray users = result.body().getJsonArray("result");
                            testContext.assertEquals(1, users.size(), "There should be only one user left, linked to exactly one merged UserAppConf");
                            final JsonObject principalUser = users.getJsonObject(0);
                            testContext.assertEquals("userToKeep5A", principalUser.getString("owner"), "The principal's own oldest UserAppConf should be kept as canonical");
                            testContext.assertEquals("fr", principalUser.getString("language"), "The canonical's own property should be preserved");
                            testContext.assertEquals("timeline-config", principalUser.getString("timeline"), "The canonical's own timeline should be preserved");
                            testContext.assertEquals("quiet-hours-principal-duplicate", principalUser.getString("quietHours"), "One quietHours value from the duplicates should be recovered");
                            testContext.assertEquals("Europe/Paris", principalUser.getString("timezone"), "Timezone should be recovered from the same duplicate as quietHours");
                            async.countDown();
                        } else {
                            testContext.fail("Could not fetch users with the same ine");
                        }
                    });
                neo4j.execute(
                        "MATCH (uac:UserAppConf) WHERE uac.tag STARTS WITH 'unitTest5' return uac",
                        new JsonObject(),
                        result -> {
                            if ("ok".equals(result.body().getString("status"))) {
                                final JsonArray userAppConfs = result.body().getJsonArray("result");
                                testContext.assertEquals(1, userAppConfs.size(), "All duplicate UserAppConf nodes must be merged, with no orphan left");
                                async.countDown();
                            } else {
                                testContext.fail("Could not fetch UserAppConf nodes");
                            }
                        });
            }));
        }));
    }

    /**
     * <h2>Goal</h2>
     * <p>Test that when only the principal user has one UserAppConf, it is preserved whatever its language, timeline
     * and quietHours fields are.</p>
     */
    @Test
    public void testMergeSameINEKeepsSinglePrincipalUserAppConf(final TestContext testContext) {
        final String inePrefix = "my-duplicated-ine-6";
        final Async async = testContext.async();
        prepareOldWithoutUserAppConfAndPrincipalWithOneUserAppConf(inePrefix).onComplete(testContext.asyncAssertSuccess(h -> {
            duplicateUsers.mergeSameINE(true, testContext.asyncAssertSuccess(e -> {
                neo4j.execute(
                        "MATCH (u:User)-[:PREFERS]->(uac:UserAppConf) WHERE u.tag IN ['unitTest6-rich', 'unitTest6-quiet-hours', 'unitTest6-minimal'] " +
                                "RETURN u.tag as tag, uac.owner as owner, uac.language as language, uac.timeline as timeline, uac.quietHours as quietHours, uac.timezone as timezone ORDER BY tag",
                        new JsonObject(),
                        result -> {
                            if ("ok".equals(result.body().getString("status"))) {
                                final JsonArray users = result.body().getJsonArray("result");
                                testContext.assertEquals(3, users.size(), "Each merged user should keep exactly one UserAppConf");
                                testContext.assertEquals("unitTest6-minimal", users.getJsonObject(0).getString("tag"));
                                testContext.assertEquals("principal-minimal", users.getJsonObject(0).getString("owner"));
                                testContext.assertNull(users.getJsonObject(0).getString("language"));
                                testContext.assertNull(users.getJsonObject(0).getString("timeline"));
                                testContext.assertNull(users.getJsonObject(0).getString("quietHours"));
                                testContext.assertEquals("unitTest6-quiet-hours", users.getJsonObject(1).getString("tag"));
                                testContext.assertEquals("quiet-hours-principal", users.getJsonObject(1).getString("quietHours"));
                                testContext.assertEquals("Europe/Paris", users.getJsonObject(1).getString("timezone"));
                                testContext.assertEquals("unitTest6-rich", users.getJsonObject(2).getString("tag"));
                                testContext.assertEquals("fr", users.getJsonObject(2).getString("language"));
                                testContext.assertEquals("timeline-principal", users.getJsonObject(2).getString("timeline"));
                                async.complete();
                            } else {
                                testContext.fail("Could not fetch users with principal UserAppConf");
                            }
                        });
            }));
        }));
    }

    /**
     * <h2>Goal</h2>
     * <p>Test the nominal merge: old user's quietHours is copied into the principal user's language/timeline node.</p>
     */
    @Test
    public void testMergeSameINEMergesOldQuietHoursIntoPrincipalRichUserAppConf(final TestContext testContext) {
        final String ine = "my-duplicated-ine-7";
        final Async async = testContext.async(2);
        prepareOldQuietHoursAndPrincipalRichUserAppConf(ine).onComplete(testContext.asyncAssertSuccess(h -> {
            duplicateUsers.mergeSameINE(true, testContext.asyncAssertSuccess(e -> {
                neo4j.execute(
                        "MATCH (u:User{ine:{ine}, tag: 'unitTest7'})-[r:PREFERS]->(uac:UserAppConf) RETURN uac.owner as owner, uac.language as language, uac.timeline as timeline, uac.quietHours as quietHours, uac.timezone as timezone",
                        new JsonObject().put("ine", ine),
                        result -> {
                            if ("ok".equals(result.body().getString("status"))) {
                                final JsonArray users = result.body().getJsonArray("result");
                                testContext.assertEquals(1, users.size(), "There should be one merged UserAppConf");
                                final JsonObject userAppConf = users.getJsonObject(0);
                                testContext.assertEquals("principal-rich", userAppConf.getString("owner"));
                                testContext.assertEquals("fr", userAppConf.getString("language"));
                                testContext.assertEquals("timeline-principal", userAppConf.getString("timeline"));
                                testContext.assertEquals("quiet-hours-old", userAppConf.getString("quietHours"));
                                testContext.assertEquals("Europe/Paris", userAppConf.getString("timezone"));
                                async.countDown();
                            } else {
                                testContext.fail("Could not fetch merged UserAppConf");
                            }
                        });
                neo4j.execute(
                        "MATCH (uac:UserAppConf) WHERE uac.tag STARTS WITH 'unitTest7' RETURN uac",
                        new JsonObject(),
                        result -> {
                            if ("ok".equals(result.body().getString("status"))) {
                                testContext.assertEquals(1, result.body().getJsonArray("result").size(), "The old UserAppConf should be deleted after merge");
                                async.countDown();
                            } else {
                                testContext.fail("Could not fetch UserAppConf nodes");
                            }
                        });
            }));
        }));
    }

    /**
     * <h2>Goal</h2>
     * <p>Test the nominal cleanup when the old user already has several quietHours-only UserAppConf nodes.</p>
     */
    @Test
    public void testMergeSameINEMergesOneQuietHoursFromSeveralOldUserAppConfs(final TestContext testContext) {
        final String ine = "my-duplicated-ine-8";
        final Async async = testContext.async(2);
        prepareOldSeveralQuietHoursAndPrincipalRichUserAppConf(ine).onComplete(testContext.asyncAssertSuccess(h -> {
            duplicateUsers.mergeSameINE(true, testContext.asyncAssertSuccess(e -> {
                neo4j.execute(
                        "MATCH (u:User{ine:{ine}, tag: 'unitTest8'})-[r:PREFERS]->(uac:UserAppConf) RETURN uac.owner as owner, uac.language as language, uac.timeline as timeline, uac.quietHours as quietHours, uac.timezone as timezone",
                        new JsonObject().put("ine", ine),
                        result -> {
                            if ("ok".equals(result.body().getString("status"))) {
                                final JsonArray users = result.body().getJsonArray("result");
                                testContext.assertEquals(1, users.size(), "There should be one merged UserAppConf");
                                final JsonObject userAppConf = users.getJsonObject(0);
                                testContext.assertEquals("principal-rich-multiple", userAppConf.getString("owner"));
                                testContext.assertEquals("fr", userAppConf.getString("language"));
                                testContext.assertEquals("timeline-principal-multiple", userAppConf.getString("timeline"));
                                testContext.assertTrue(userAppConf.getString("quietHours").startsWith("quiet-hours-old-"), "One old quietHours value should be recovered");
                                testContext.assertTrue(userAppConf.getString("timezone").startsWith("Europe/"), "Timezone should be recovered from a duplicate quietHours node");
                                async.countDown();
                            } else {
                                testContext.fail("Could not fetch merged UserAppConf");
                            }
                        });
                neo4j.execute(
                        "MATCH (uac:UserAppConf) WHERE uac.tag STARTS WITH 'unitTest8' RETURN uac",
                        new JsonObject(),
                        result -> {
                            if ("ok".equals(result.body().getString("status"))) {
                                testContext.assertEquals(1, result.body().getJsonArray("result").size(), "All old duplicate UserAppConf nodes should be deleted after merge");
                                async.countDown();
                            } else {
                                testContext.fail("Could not fetch UserAppConf nodes");
                            }
                        });
            }));
        }));
    }

    /**
     * <h2>Goal</h2>
     * <p>Test the nominal cleanup when the old user has language/timeline and the principal user already has several
     * quietHours-only UserAppConf nodes.</p>
     */
    @Test
    public void testMergeSameINEMergesOnePrincipalQuietHoursIntoOldRichUserAppConf(final TestContext testContext) {
        final String ine = "my-duplicated-ine-9";
        final Async async = testContext.async(2);
        prepareOldRichAndPrincipalSeveralQuietHoursUserAppConfs(ine).onComplete(testContext.asyncAssertSuccess(h -> {
            duplicateUsers.mergeSameINE(true, testContext.asyncAssertSuccess(e -> {
                neo4j.execute(
                        "MATCH (u:User{ine:{ine}, tag: 'unitTest9'})-[r:PREFERS]->(uac:UserAppConf) RETURN uac.owner as owner, uac.language as language, uac.timeline as timeline, uac.quietHours as quietHours, uac.timezone as timezone",
                        new JsonObject().put("ine", ine),
                        result -> {
                            if ("ok".equals(result.body().getString("status"))) {
                                final JsonArray users = result.body().getJsonArray("result");
                                testContext.assertEquals(1, users.size(), "There should be one merged UserAppConf");
                                final JsonObject userAppConf = users.getJsonObject(0);
                                testContext.assertEquals("old-rich-multiple", userAppConf.getString("owner"));
                                testContext.assertEquals("fr", userAppConf.getString("language"));
                                testContext.assertEquals("timeline-old-multiple", userAppConf.getString("timeline"));
                                testContext.assertTrue(userAppConf.getString("quietHours").startsWith("quiet-hours-principal-"), "One principal quietHours value should be recovered");
                                testContext.assertTrue(userAppConf.getString("timezone").startsWith("Europe/"), "Timezone should be recovered from a duplicate quietHours node");
                                async.countDown();
                            } else {
                                testContext.fail("Could not fetch merged UserAppConf");
                            }
                        });
                neo4j.execute(
                        "MATCH (uac:UserAppConf) WHERE uac.tag STARTS WITH 'unitTest9' RETURN uac",
                        new JsonObject(),
                        result -> {
                            if ("ok".equals(result.body().getString("status"))) {
                                testContext.assertEquals(1, result.body().getJsonArray("result").size(), "All principal duplicate UserAppConf nodes should be deleted after merge");
                                async.countDown();
                            } else {
                                testContext.fail("Could not fetch UserAppConf nodes");
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

    /**
     * Creates 2 users as follows :
     * <ol>
     *     <li>(:User{id: 'userToKeep3', source: 'AAF', ine: ine, activationCode: 'toto'})-[:PREFERS]->(:UserAppConf{tag: 'unitTest3', owner: 'userToKeep3'})</li>
     *     <li>(:User{id: 'userToRemove3', source: 'MANUAL', ine: ine})-[:PREFERS]->(:UserAppConf{tag: 'unitTest3-old', owner: 'userToRemove3'})</li>
     * </ol>
     * @param ine The ine to use for both users
     */
    public static Future<Void> prepareSameINEUsersWithAUserAppConfEach(final String ine) {
        final JsonObject params = new JsonObject().put("ine", ine);
        final Promise<Void> promise = Promise.promise();
        TransactionHelper txl = null;
        try {
            txl = TransactionManager.getTransaction();
            txl.add("match (u) detach delete u", new JsonObject());
            txl.add("create (u1:User{id: 'userToKeep3', source: 'AAF', activationCode: 'toto', ine: {ine}, tag: 'unitTest3', activated:true})-[:PREFERS]->(:UserAppConf{tag: 'unitTest3', owner: 'userToKeep3'})", params);
            txl.add("create (u2:User{id: 'userToRemove3', source: 'MANUAL', ine: {ine}, tag: 'unitTest3'})-[:PREFERS]->(:UserAppConf{tag: 'unitTest3-old', owner: 'userToRemove3'})", params);
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
     *     <li>(:User{id: 'userToKeep4', source: 'AAF', ine: ine, activationCode: 'toto'})</li>
     *     <li>(:User{id: 'userToRemove4', source: 'MANUAL', ine: ine})-[:PREFERS]->(:UserAppConf{tag: 'unitTest4', owner: 'userToRemove4'})</li>
     * </ol>
     * @param ine The ine to use for both users
     */
    public static Future<Void> prepareSameINEUsersWithOnlyOneUserAppConf(final String ine) {
        final JsonObject params = new JsonObject().put("ine", ine);
        final Promise<Void> promise = Promise.promise();
        TransactionHelper txl = null;
        try {
            txl = TransactionManager.getTransaction();
            txl.add("match (u) detach delete u", new JsonObject());
            txl.add("create (u1:User{id: 'userToKeep4', source: 'AAF', activationCode: 'toto', ine: {ine}, tag: 'unitTest4'})", params);
            txl.add("create (u2:User{id: 'userToRemove4', source: 'MANUAL', ine: {ine}, tag: 'unitTest4'})-[:PREFERS]->(:UserAppConf{tag: 'unitTest4', owner: 'userToRemove4'})", params);
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
     *     <li>(:User{id: 'userToKeep5', source: 'AAF', ine: ine, activationCode: 'toto'}) with two UserAppConf nodes.</li>
     *     <li>(:User{id: 'userToRemove5', source: 'MANUAL', ine: ine}) with one UserAppConf node.</li>
     * </ol>
     * @param ine The ine to use for both users
     */
    public static Future<Void> prepareSameINEUsersWithAlreadyDuplicatedPreferences(final String ine) {
        final JsonObject params = new JsonObject().put("ine", ine);
        final Promise<Void> promise = Promise.promise();
        TransactionHelper txl = null;
        try {
            txl = TransactionManager.getTransaction();
            txl.add("match (u) detach delete u", new JsonObject());
                txl.add("create (u1:User{id: 'userToKeep5', source: 'AAF', activationCode: 'toto', ine: {ine}, tag: 'unitTest5'}) " +
                    "create (u1)-[:PREFERS]->(:UserAppConf{tag: 'unitTest5-a', owner: 'userToKeep5A', language: 'fr', timeline: 'timeline-config'}) " +
                    "create (u1)-[:PREFERS]->(:UserAppConf{tag: 'unitTest5-b', owner: 'userToKeep5B', quietHours: 'quiet-hours-principal-duplicate', timezone: 'Europe/Paris'})", params);
                txl.add("create (u2:User{id: 'userToRemove5', source: 'MANUAL', ine: {ine}, tag: 'unitTest5'})-[:PREFERS]->(:UserAppConf{tag: 'unitTest5-old', owner: 'userToRemove5', quietHours: 'quiet-hours-old-user'})", params);
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
     * Creates three merge pairs where only the principal user has one UserAppConf, with different combinations of
     * language, timeline and quietHours fields.
     * @param inePrefix The ine prefix to use for the three pairs
     */
    public static Future<Void> prepareOldWithoutUserAppConfAndPrincipalWithOneUserAppConf(final String inePrefix) {
        final JsonObject params = new JsonObject().put("inePrefix", inePrefix);
        final Promise<Void> promise = Promise.promise();
        TransactionHelper txl = null;
        try {
            txl = TransactionManager.getTransaction();
            txl.add("match (u) detach delete u", new JsonObject());
            txl.add("create (u1:User{id: 'userToKeep6A', source: 'AAF', activationCode: 'toto', ine: {inePrefix} + '-a', tag: 'unitTest6-rich'})-[:PREFERS]->(:UserAppConf{tag: 'unitTest6-rich', owner: 'principal-rich', language: 'fr', timeline: 'timeline-principal'})", params);
            txl.add("create (u2:User{id: 'userToRemove6A', source: 'MANUAL', ine: {inePrefix} + '-a', tag: 'unitTest6-rich'})", params);
            txl.add("create (u1:User{id: 'userToKeep6B', source: 'AAF', activationCode: 'toto', ine: {inePrefix} + '-b', tag: 'unitTest6-quiet-hours'})-[:PREFERS]->(:UserAppConf{tag: 'unitTest6-quiet-hours', owner: 'principal-quiet-hours', quietHours: 'quiet-hours-principal', timezone: 'Europe/Paris'})", params);
            txl.add("create (u2:User{id: 'userToRemove6B', source: 'MANUAL', ine: {inePrefix} + '-b', tag: 'unitTest6-quiet-hours'})", params);
            txl.add("create (u1:User{id: 'userToKeep6C', source: 'AAF', activationCode: 'toto', ine: {inePrefix} + '-c', tag: 'unitTest6-minimal'})-[:PREFERS]->(:UserAppConf{tag: 'unitTest6-minimal', owner: 'principal-minimal'})", params);
            txl.add("create (u2:User{id: 'userToRemove6C', source: 'MANUAL', ine: {inePrefix} + '-c', tag: 'unitTest6-minimal'})", params);
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
     * Creates one old user with quietHours and one principal user with language/timeline.
     * @param ine The ine to use for both users
     */
    public static Future<Void> prepareOldQuietHoursAndPrincipalRichUserAppConf(final String ine) {
        final JsonObject params = new JsonObject().put("ine", ine);
        final Promise<Void> promise = Promise.promise();
        TransactionHelper txl = null;
        try {
            txl = TransactionManager.getTransaction();
            txl.add("match (u) detach delete u", new JsonObject());
            txl.add("create (u1:User{id: 'userToKeep7', source: 'AAF', activationCode: 'toto', ine: {ine}, tag: 'unitTest7'})-[:PREFERS]->(:UserAppConf{tag: 'unitTest7-rich', owner: 'principal-rich', language: 'fr', timeline: 'timeline-principal'})", params);
            txl.add("create (u2:User{id: 'userToRemove7', source: 'MANUAL', ine: {ine}, tag: 'unitTest7'})-[:PREFERS]->(:UserAppConf{tag: 'unitTest7-old', owner: 'old-quiet-hours', quietHours: 'quiet-hours-old', timezone: 'Europe/Paris'})", params);
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
     * Creates one old user with three quietHours UserAppConf nodes and one principal user with language/timeline.
     * @param ine The ine to use for both users
     */
    public static Future<Void> prepareOldSeveralQuietHoursAndPrincipalRichUserAppConf(final String ine) {
        final JsonObject params = new JsonObject().put("ine", ine);
        final Promise<Void> promise = Promise.promise();
        TransactionHelper txl = null;
        try {
            txl = TransactionManager.getTransaction();
            txl.add("match (u) detach delete u", new JsonObject());
            txl.add("create (u1:User{id: 'userToKeep8', source: 'AAF', activationCode: 'toto', ine: {ine}, tag: 'unitTest8'})-[:PREFERS]->(:UserAppConf{tag: 'unitTest8-rich', owner: 'principal-rich-multiple', language: 'fr', timeline: 'timeline-principal-multiple'})", params);
            txl.add("create (u2:User{id: 'userToRemove8', source: 'MANUAL', ine: {ine}, tag: 'unitTest8'}) " +
                    "create (u2)-[:PREFERS]->(:UserAppConf{tag: 'unitTest8-old-a', owner: 'old-quiet-hours-a', quietHours: 'quiet-hours-old-a', timezone: 'Europe/Paris'}) " +
                    "create (u2)-[:PREFERS]->(:UserAppConf{tag: 'unitTest8-old-b', owner: 'old-quiet-hours-b', quietHours: 'quiet-hours-old-b', timezone: 'Europe/Madrid'}) " +
                    "create (u2)-[:PREFERS]->(:UserAppConf{tag: 'unitTest8-old-c', owner: 'old-quiet-hours-c', quietHours: 'quiet-hours-old-c', timezone: 'Europe/Rome'})", params);
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
     * Creates one old user with language/timeline and one principal user with three quietHours UserAppConf nodes.
     * @param ine The ine to use for both users
     */
    public static Future<Void> prepareOldRichAndPrincipalSeveralQuietHoursUserAppConfs(final String ine) {
        final JsonObject params = new JsonObject().put("ine", ine);
        final Promise<Void> promise = Promise.promise();
        TransactionHelper txl = null;
        try {
            txl = TransactionManager.getTransaction();
            txl.add("match (u) detach delete u", new JsonObject());
            txl.add("create (u1:User{id: 'userToKeep9', source: 'AAF', activationCode: 'toto', ine: {ine}, tag: 'unitTest9'}) " +
                    "create (u1)-[:PREFERS]->(:UserAppConf{tag: 'unitTest9-principal-a', owner: 'principal-quiet-hours-a', quietHours: 'quiet-hours-principal-a', timezone: 'Europe/Paris'}) " +
                    "create (u1)-[:PREFERS]->(:UserAppConf{tag: 'unitTest9-principal-b', owner: 'principal-quiet-hours-b', quietHours: 'quiet-hours-principal-b', timezone: 'Europe/Madrid'}) " +
                    "create (u1)-[:PREFERS]->(:UserAppConf{tag: 'unitTest9-principal-c', owner: 'principal-quiet-hours-c', quietHours: 'quiet-hours-principal-c', timezone: 'Europe/Rome'})", params);
            txl.add("create (u2:User{id: 'userToRemove9', source: 'MANUAL', ine: {ine}, tag: 'unitTest9'})-[:PREFERS]->(:UserAppConf{tag: 'unitTest9-rich', owner: 'old-rich-multiple', language: 'fr', timeline: 'timeline-old-multiple'})", params);
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
