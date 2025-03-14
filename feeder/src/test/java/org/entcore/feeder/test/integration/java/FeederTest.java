package org.entcore.feeder.test.integration.java;

import io.vertx.core.AsyncResult;
import io.vertx.core.Context;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.entcore.common.neo4j.Neo4j;
import org.entcore.common.schema.Source;
import org.entcore.feeder.Feeder;
import org.entcore.feeder.ManualFeeder;
import org.entcore.common.neo4j.TransactionHelper;
import org.entcore.test.TestHelper;
import org.junit.After;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.testcontainers.containers.Neo4jContainer;

import java.util.Arrays;

@RunWith(VertxUnitRunner.class)
public class FeederTest {
    private static final TestHelper test = TestHelper.helper();
    @ClassRule
    public static Neo4jContainer<?> neo4jContainer = test.database().createNeo4jContainer();

    static {
        System.setProperty("java.util.logging.SimpleFormatter.format", "%1$tY-%1$tm-%1$td %1$tH:%1$tM:%1$tS %4$s %5$s%6$s%n");
    }

    @BeforeClass
    public static void setUp(TestContext context) throws Exception {
        test.database().initNeo4j(context, neo4jContainer);
        final Feeder feeder = new Feeder();
        final Context context1 = test.vertx().getOrCreateContext();
        final String path = FeederTest.class.getClassLoader().getResource("aaf").getPath();
        final JsonObject config = new JsonObject()
                .put("log-details", true)
                .put("postImport", true)
                .put("apply-communication-rules", true)
                .put("feeder", "AAF")
                .put("import-files", path)
                .put("delete-user-delay", 10000l)
                .put("pre-delete-user-delay", 1000l)
                .put("delete-cron", "0 0 0 * * ? 2099")
                .put("pre-delete-cron", "0 0 0 * * ? 2099");
        context1.config().mergeIn(config);
        feeder.init(test.vertx(), context1);
        feeder.start();
        test.vertx().eventBus().consumer(Feeder.FEEDER_ADDRESS, feeder);
    }

    @After
    public void cleanUp() {
        test.database().executeNeo4j("MATCH (n) DETACH DELETE n", new JsonObject());
    }

    @Test
    public void testShouldImportAaf(final TestContext context) {
        final Async async = context.async();
        final EventBus eb = test.vertx().eventBus();
        eb.request(Feeder.FEEDER_ADDRESS, new JsonObject().put("action", "import"), (AsyncResult<Message<JsonObject>> message) -> {
            context.assertEquals("ok", message.result().body().getString("status"));
            //wait post import
            test.vertx().setTimer(1000, e -> {
                async.complete();
            });
        });
    }

    @Test
    public void testShouldImportAafAndRemoveUser(final TestContext context) {
        final Async async = context.async();
        final EventBus eb = test.vertx().eventBus();
        final String userId = "1";
        final JsonObject params = new JsonObject().put("userId", userId).put("classId", "1").put("structureId", "1");
        test.database().executeNeo4j("MERGE (u:User { id : {userId}})-[r:IN]->(cpg:ProfileGroup)-[:DEPENDS]->(pg:ProfileGroup)-[:DEPENDS]->(s:Structure {id: {structureId}})  WITH cpg,s, pg MERGE pg-[:HAS_PROFILE]->(p:Profile)<-[:HAS_PROFILE]-(dpg:DefaultProfileGroup) WITH cpg MERGE (cpg)-[:DEPENDS]->(c:Class  {id : {classId}}) RETURN cpg", params).onComplete(context.asyncAssertSuccess(res2 -> {
            //remove user from class (attach user to defaultgroup
            eb.request(Feeder.FEEDER_ADDRESS, new JsonObject().put("action", "manual-remove-user").put("classId", "1").put("userId", userId), (AsyncResult<Message<JsonObject>> res3) -> {
                context.assertEquals("ok", res3.result().body().getString("status"));
                context.assertEquals(1, res3.result().body().getJsonArray("results").getJsonArray(0).size());
                //wait for query
                test.vertx().setTimer(300, e -> {
                    //recreate the link between u AND cpg (when aaf)
                    test.database().executeNeo4j("MATCH (u:User { id : {userId}}), (cpg:ProfileGroup)-[:DEPENDS]->(pg:ProfileGroup)-[:DEPENDS]->(s:Structure {id: {structureId}}) MERGE u-[r:IN]->cpg RETURN ID(u)", params).onComplete(context.asyncAssertSuccess(res20 -> {
                        final TransactionHelper transaction = new TransactionHelper(Neo4j.getInstance(), Source.UNKNOWN);
                        ManualFeeder.applyRemoveUserFromStructure(userId, null, "1", null, transaction);
                        transaction.commit((Message<JsonObject> message2) -> {
                            context.assertEquals("ok", message2.body().getString("status"));
                            context.assertEquals(1, message2.body().getJsonArray("results").getJsonArray(0).size());
                            async.complete();
                        });
                    }));
                });
            });
        }));
    }

    /**
     * This test aims at verifying that, when a user is manually added to a class :
     * - a relation between the user and the class profile group is created and marked with a MANUAL source
     * - no additional relation between the user and the structure profile group is created if one already exists
     *
     * @param context test context to handle assertion in asynchronous environment
     */
    @Test
    public void testShouldAddUserToClass(final TestContext context) {
        final Async async = context.async();
        final String prepareDatabaseQuery = "" +
                "MERGE (u:User {id : {userId}})-[inStructureProfileGroup:IN {labelForCurrentTest : 'originalRelation'}]->(spg:ProfileGroup)-[:DEPENDS]->(s:Structure {id: {structureId}}) " +
                "WITH spg, s " +
                "MERGE (cpg:ProfileGroup)-[:DEPENDS]->(spg)-[:HAS_PROFILE]->(p:Profile) " +
                "WITH cpg, s " +
                "MERGE (cpg)-[:DEPENDS]->(c:Class  {id : {classId}})-[:BELONGS]->(s) " +
                "RETURN cpg";
        final JsonObject params = new JsonObject().put("userId", "user-id-1").put("classId", "class-id-1").put("structureId", "structure-id-1");
        // prepare database
        test.database().executeNeo4j(prepareDatabaseQuery, params).onComplete(context.asyncAssertSuccess(preparedDatabaseResult -> {
            // execute method to be tested
            test.vertx().eventBus().request(Feeder.FEEDER_ADDRESS, new JsonObject().put("action", "manual-add-user").put("userId", "user-id-1").put("classId", "class-id-1"), (AsyncResult<Message<JsonObject>> feederResponse) -> {
                context.assertEquals("ok", feederResponse.result().body().getString("status"));
                context.assertEquals(1, feederResponse.result().body().getJsonArray("results").getJsonArray(0).size());
                // wait for query to complete
                test.vertx().setTimer(300, timerComplete -> {
                    final String verificationQuery = "" +
                            "MATCH (u:User {id : {userId}})-[newlyCreatedIn:IN]->(cpg:ProfileGroup)-[:DEPENDS]->(c:Class {id : {classId}}) " +
                            "WITH u, newlyCreatedIn, c " +
                            "MATCH (u)-[inStructureProfileGroup]->(spg:ProfileGroup)-[:DEPENDS]->(s:Structure{id : {structureId}}) " +
                            "RETURN u, newlyCreatedIn, c, inStructureProfileGroup";
                    test.database().executeNeo4j(verificationQuery, params).onComplete(context.asyncAssertSuccess(verificationQueryResults -> {
                        // verify method execution
                        context.assertEquals(1, verificationQueryResults.size());
                        context.assertEquals(4, verificationQueryResults.getJsonObject(0).size());
                        context.assertEquals("user-id-1", verificationQueryResults.getJsonObject(0).getJsonObject("u").getJsonObject("data").getString("id"));
                        context.assertEquals("MANUAL", verificationQueryResults.getJsonObject(0).getJsonObject("newlyCreatedIn").getJsonObject("data").getString("source"));
                        context.assertEquals("class-id-1", verificationQueryResults.getJsonObject(0).getJsonObject("c").getJsonObject("data").getString("id"));
                        context.assertEquals("originalRelation", verificationQueryResults.getJsonObject(0).getJsonObject("inStructureProfileGroup").getJsonObject("data").getString("labelForCurrentTest"));
                    }));
                    async.complete();
                });
            });
        }));
    }

    /**
     * This test aims at verifying that local admin rights are given to a user for the specified structure and all the related sub-structures
     *
     * @param context test context to handle assertion in asynchronous environment
     */
    @Test
    public void testShouldAddFunctionLocalAdmin(final TestContext context) {
        final Async async = context.async();
        final String prepareDatabaseQuery = "" +
                "MERGE (u:User {id : {userId}}) " +
                "MERGE (f:Function {externalId : {functionCode}}) " +
                "MERGE (mainStructure:Structure {id : {mainStructureId}}) " +
                "MERGE (subStructure1:Structure {id : {subStructureId1}})-[:HAS_ATTACHMENT]->(mainStructure) " +
                "MERGE (subStructure2:Structure {id : {subStructureId2}})-[:HAS_ATTACHMENT]->(mainStructure) ";
        final JsonObject params = new JsonObject()
                .put("userId", "user-id-1")
                .put("functionCode", "ADMIN_LOCAL")
                .put("mainStructureId", "main-structure-id")
                .put("subStructureId1", "sub-structure-id-1")
                .put("subStructureId2", "sub-structure-id-2");
        // prepare database
        test.database().executeNeo4j(prepareDatabaseQuery, params).onComplete(context.asyncAssertSuccess(preparedDatabaseResult -> {
            // execute method to be tested
            test.vertx().eventBus().request(Feeder.FEEDER_ADDRESS, new JsonObject()
                    .put("action", "manual-add-user-function")
                    .put("userId", "user-id-1")
                    .put("function", "ADMIN_LOCAL")
                    .put("scope", new JsonArray(Arrays.asList("main-structure-id")))
                    .put("inherit", "s"), (AsyncResult<Message<JsonObject>> feederResponse) -> {
                context.assertEquals("ok", feederResponse.result().body().getString("status"));
                context.assertEquals(3, feederResponse.result().body().getJsonArray("results").getJsonArray(1).size(), "Three ids of the newly created function group should be returned");
                // wait for query to complete
                test.vertx().setTimer(300, timerComplete -> {
                    final String verificationQuery = "" +
                            "MATCH (:User {id : {userId}})-[hasFunction:HAS_FUNCTION]->(:Function {externalId : {functionCode}}) RETURN hasFunction";
                    test.database().executeNeo4j(verificationQuery, params).onComplete(context.asyncAssertSuccess(verificationQueryResults -> {
                        // verify method execution
                        context.assertEquals(1, verificationQueryResults.size());
                        context.assertEquals(1, verificationQueryResults.getJsonObject(0).size());
                        context.assertTrue(verificationQueryResults.getJsonObject(0).getJsonObject("hasFunction").getJsonObject("data").getJsonArray("scope").getList().containsAll(Arrays.asList("main-structure-id", "sub-structure-id-1", "sub-structure-id-2")));
                    }));
                    final String verificationQueryFunctionGroup = "" +
                            "MATCH (:User {id : {userId}})-[:IN]->(functionGroup:FunctionGroup)-[:DEPENDS]->(:Structure) RETURN collect(functionGroup.externalId) as functionGroupIds";
                    test.database().executeNeo4j(verificationQueryFunctionGroup, params).onComplete(context.asyncAssertSuccess(verificationQueryResults -> {
                        // verify method execution
                        context.assertEquals(1, verificationQueryResults.size());
                        context.assertEquals(3, verificationQueryResults.getJsonObject(0).getJsonArray("functionGroupIds").size());
                        context.assertTrue(verificationQueryResults.getJsonObject(0).getJsonArray("functionGroupIds").getList().containsAll(Arrays.asList("main-structure-id-ADMIN_LOCAL", "sub-structure-id-1-ADMIN_LOCAL", "sub-structure-id-2-ADMIN_LOCAL")));
                    }));
                    async.complete();
                });
            });
        }));
    }

    /**
     * This test aims at verifying that local admin rights of a user are removed for the specified structure and all the related sub-structures
     *
     * @param context test context to handle assertion in asynchronous environment
     */
    @Test
    public void testShouldRemoveFunctionLocalAdmin(final TestContext context) {
        final Async async = context.async();
        final String prepareDatabaseQuery = "" +
                "MERGE (u:User {id : {userId}})-[:HAS_FUNCTION]->(:Function { externalId : {functionCode}}) " +
                "WITH u " +
                "MERGE (u)-[:IN]->(fg:FunctionGroup { externalId : {mainStructureFunctionGroupId}}) " +
                "MERGE (u)-[:IN]->(fg:FunctionGroup { externalId : {subStructureFunctionGroupId1}}) " +
                "MERGE (u)-[:IN]->(fg:FunctionGroup { externalId : {subStructureFunctionGroupId2}}) " +
                "WITH u, fg " +
                "MERGE (u)-[:COMMUNIQUE]->(fg) " +
                "MERGE (u)-[:COMMUNIQUE]->(fg) " +
                "MERGE (u)-[:COMMUNIQUE]->(fg) ";
        final JsonObject params = new JsonObject()
                .put("userId", "user-id-1")
                .put("functionCode", "ADMIN_LOCAL")
                .put("mainStructureFunctionGroupId", "main-structure-ADMIN_LOCAL")
                .put("subStructureFunctionGroupId1", "sub-structure-1-ADMIN_LOCAL")
                .put("subStructureFunctionGroupId2", "sub-structure-2-ADMIN_LOCAL");
        // prepare database
        test.database().executeNeo4j(prepareDatabaseQuery, params).onComplete(context.asyncAssertSuccess(preparedDatabaseResult -> {
            // execute method to be tested
            test.vertx().eventBus().request(Feeder.FEEDER_ADDRESS, new JsonObject()
                    .put("action", "manual-remove-user-function")
                    .put("userId", "user-id-1")
                    .put("function", "ADMIN_LOCAL"), (AsyncResult<Message<JsonObject>> feederResponse) -> {
                context.assertEquals("ok", feederResponse.result().body().getString("status"));
                // wait for query to complete
                test.vertx().setTimer(300, timerComplete -> {
                    final String verificationQuery = "" +
                            "OPTIONAL MATCH (:User)-[hasFunction:HAS_FUNCTION]->(:Function) " +
                            "OPTIONAL MATCH (:User)-[in:IN]->(:FunctionGroup) " +
                            "OPTIONAL MATCH (:User)-[communique:COMMUNIQUE]->(:FunctionGroup) " +
                            "RETURN count(hasFunction) as nbHasF, count(in) as nbIn, count(communique) as nbComm";
                    test.database().executeNeo4j(verificationQuery, params).onComplete(context.asyncAssertSuccess(verificationQueryResults -> {
                        // verify method execution
                        context.assertEquals(1, verificationQueryResults.size());
                        context.assertEquals(0, verificationQueryResults.getJsonObject(0).getInteger("nbHasF"));
                        context.assertEquals(0, verificationQueryResults.getJsonObject(0).getInteger("nbIn"));
                        context.assertEquals(0, verificationQueryResults.getJsonObject(0).getInteger("nbComm"));
                    }));
                    async.complete();
                });
            });
        }));
    }
}
