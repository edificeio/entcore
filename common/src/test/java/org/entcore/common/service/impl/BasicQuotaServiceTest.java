package org.entcore.common.service.impl;

import io.vertx.core.Vertx;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import static org.apache.commons.lang3.StringUtils.isEmpty;
import org.entcore.common.events.EventStoreFactory;
import org.entcore.common.neo4j.Neo4j;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.testcontainers.containers.Neo4jContainer;
import org.testcontainers.utility.DockerImageName;

@RunWith(VertxUnitRunner.class)
public class BasicQuotaServiceTest {
    // private static final TestHelper test = TestHelper.helper();
    @ClassRule
    public static Neo4jContainer<?> neo4jContainer = createNeo4jContainer();
    private static Neo4j neo4j;
    private static BasicQuotaService basicQuotaService;
    private static final Vertx vertx = Vertx.vertx();

    @BeforeClass
    public static void setUp(TestContext context) throws Exception {
        Vertx vertx = Vertx.vertx();
        EventStoreFactory.getFactory().setVertx(vertx);
        basicQuotaService = new BasicQuotaService();
        initNeo4j(context, neo4jContainer);
        final String base = neo4jContainer.getHttpUrl() + "/db/data/";
        final JsonObject neo4jConfig = new JsonObject()
                .put("server-uri", base).put("poolSize", 1);
        neo4j = Neo4j.getInstance();
        neo4j.init(vertx, neo4jConfig
                .put("server-uri", base)
                .put("ignore-empty-statements-error", false));
    }

    public static void initNeo4j(TestContext context, Neo4jContainer<?> neo4jContainer) {
        final String base = neo4jContainer.getHttpUrl() + "/db/data/";
        final JsonObject config = new JsonObject().put("server-uri", base).put("poolSize", 1);
        final Neo4j neo4j = Neo4j.getInstance();
        neo4j.init(vertx, config);
        vertx.sharedData().getLocalMap("server").put("neo4jConfig", config.encode());
    }

    public static Neo4jContainer<?> createNeo4jContainer() {
        final DockerImageName imageName;
        if("true".equalsIgnoreCase(System.getenv("IS_M1")) || "aarch64".equalsIgnoreCase(System.getProperty("os.arch"))) {
            imageName = DockerImageName.parse("opendigitaleducation/neo4j:3.1.9-arm").asCompatibleSubstituteFor("neo4j");
        } else {
            imageName = DockerImageName.parse("neo4j:3.1");
        }
        final Neo4jContainer container = new Neo4jContainer(imageName);
        return container.withoutAuthentication()//
                .withNeo4jConfig("cypher.default_language_version", "2.3");
    }

    /**
     * <h1>Goal</h1>
     * <p>Test that if a user doesn't have any quota then a call to {@code init} will create
     * <b><u>one and only one</u></b> userbook node.</p>
     * @param testContext Test context
     */
    @Test
    public void testInitQuotaOfUserWithoutUserBook(final TestContext testContext) {
        final String userId = "userIdTestInitQuotaOfUserWithoutUserBook";
        final Async async = testContext.async(3);
        prepareData(userId, null).onComplete(testContext.asyncAssertSuccess(h -> {
            basicQuotaService.init(userId);
            vertx.setTimer(1000L, e -> assertUserHasOnlyOneUserBook(userId, testContext, async));
        }));
    }

    /**
     * <h1>Goal</h1>
     * <p>Test that if a user is already linked to a UserBook node whose field userid is set to the actual id of the
     * target user then a call to {@code init} will <b><u>NOT CREATE</u></b> a new userbook node. The user will still
     * have <b><u>one and only one</u></b> UserBook node.</p>
     * @param testContext Test context
     */
    @Test
    public void testInitQuotaOfUserWithGoodUserBook(final TestContext testContext) {
        final String userId = "userIdTestInitQuotaOfUserWithGoodUserBook";
        final Async async = testContext.async(3);
        prepareData(userId, userId).onComplete(testContext.asyncAssertSuccess(h -> {
            basicQuotaService.init(userId);
            vertx.setTimer(1000L, e -> assertUserHasOnlyOneUserBook(userId, testContext, async));
        }));
    }

    /**
     * <h1>Goal</h1>
     * <p>Test that if a user is already linked to a UserBook node whose field userid is set to a value which is not
     * the actual id of the target user's id then a call to {@code init} will <b><u>NOT CREATE</u></b> a new userbook
     * node. The user will still have <b><u>one and only one</u></b> UserBook node.</p>
     * @param testContext Test context
     */
    @Test
    public void testInitQuotaOfUserWithBadUserBook(final TestContext testContext) {
        final String userId = "userIdTestInitQuotaOfUserWithBadUserBook";
        final Async async = testContext.async(3);
        prepareData(userId, "badId").onComplete(testContext.asyncAssertSuccess(h -> {
            basicQuotaService.init(userId);
            vertx.setTimer(1000L, e -> assertUserHasOnlyOneUserBook(userId, testContext, async));
        }));
    }

    private void assertUserHasOnlyOneUserBook(final String userId, final TestContext testContext, final Async async) {
        neo4j.execute(
                "MATCH (u:User{id:{userId}})-[r:USERBOOK]->(ub:UserBook) return ub.userid as ub_user_id ",
                new JsonObject().put("userId", userId),
                result -> {
                    if ("ok".equals(result.body().getString("status"))) {
                        final JsonArray ubs = result.body().getJsonArray("result");
                        testContext.assertEquals(1, ubs.size(), "There should be only one created userbook");
                        testContext.assertEquals(userId, ubs.getJsonObject(0).getString("ub_user_id"), "The created userbook's doesn't have the right user id");
                        async.complete();
                    } else {
                        testContext.fail("Could not fetch userbooks");
                    }
                });
    }

    /**
     * Creates a user and eventually a userbook linked to it.
     * @param userId Id of the user to create
     * @param ubUserId Value of the field {@code userid} to set for the UserBook node. If it is
     * {@code null} then no UserBook node is created
     * @return When this method ends
     */
    private Future<Void> prepareData(final String userId, final String ubUserId) {
        final Promise<Void> promise = Promise.promise();
        final String query;
        final JsonObject params = new JsonObject().put("userId", userId);
        if(isEmpty(ubUserId)) {
            query = "create (u1:User{id: {userId}})-[:IN]->(:ProfileGroup)-[:HAS_PROFILE]->(p:Profile)";
        } else {
            query = "create (:UserBook{userid:{ubUserId}})<-[:USERBOOK]-(u1:User{id: {userId}})-[:IN]->(:ProfileGroup)-[:HAS_PROFILE]->(p:Profile)";
            params.put("ubUserId", ubUserId);
        }
        neo4j.execute(query, params, e -> {
            if ("ok".equals(e.body().getString("status"))) {
                promise.complete();
            } else {
                promise.fail(e.body().encodePrettily());
            }
        });
        return promise.future();
    }
}
