package org.entcore.feeder.dictionary.structures;


import com.google.common.collect.Lists;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.entcore.common.events.EventStoreFactory;
import org.entcore.common.neo4j.Neo4j;
import org.entcore.feeder.utils.TransactionManager;
import org.entcore.test.TestHelper;
import org.entcore.test.preparation.*;
import org.entcore.test.preparation.Profile;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.testcontainers.containers.Neo4jContainer;

@RunWith(VertxUnitRunner.class)
public class GroupTest {

    private static final TestHelper test = TestHelper.helper();
    @ClassRule
    public static Neo4jContainer<?> neo4jContainer = test.database().createNeo4jContainer();

    static final UserTest teacher = UserTestBuilder.anUserTest().id("user.two")
            .login("user-two")
            .firstName("User").lastName("Two")
            .displayName("User Two")
            .profile(Profile.Teacher)
            .userBook(new UserBookTest("user.two", "ine.user.two", 1000, 0))
            .build();

    private static final String DIRECTOR_USER_POS = "Directeur / Chef Etablissement";
    private static final String TEACHER_USER_POS = "Enseignant / Maths";

    private static DataHelper dataHelper;

    static final UserTest adml = UserTestBuilder.anUserTest().id("user.adml")
            .login("user-adml")
            .firstName("Adée").lastName("Émelle")
            .profile(Profile.Personnel)
            .userBook(new UserBookTest("user.adml", "ine.user.adml", 1000, 0)).build();

    @BeforeClass
    public static void setUp(TestContext context) {
        final Vertx vertx = test.vertx();
        EventStoreFactory.getFactory().setVertx(vertx);
        dataHelper = DataHelper.init(context, neo4jContainer);
        TransactionManager.getInstance().setNeo4j(Neo4j.getInstance());
        prepareData().onComplete(context.asyncAssertSuccess());
    }

    @Test
    public void runLinkGroupRules_withAutolinkUsersFromPositions_shouldLink(TestContext testContext){
        String query = "MATCH (mg:ManualGroup{id: 'group-01'})-[:DEPENDS]->(s:Structure) return s, mg.nbUsers as nbUsers";
        final Async async = testContext.async();
        Group.runLinkRules().onComplete((h) -> {
            Neo4j.getInstance().execute(
                    query, new JsonObject(),
                    result -> {
                        if ("ok".equals(result.body().getString("status"))) {
                            final JsonArray userbooks = result.body().getJsonArray("result");
                            testContext.assertEquals(1, userbooks.size(), "There should be only one result");
                            final JsonObject ub = userbooks.getJsonObject(0);
                            testContext.assertEquals("1", ub.getString("nbUsers"), "The number of user in the group is not the excepted one");
                            async.complete();
                        } else {
                            testContext.fail("Could not fetch broadcast group");
                        }
                    });
        });
    }


    public static Future<Void> prepareData() {
        dataHelper
                .start()
                .withStructure(new StructureTest("my-structure-01", "my structure 01"))
                .withStructure(new StructureTest("my-structure-02", "my structure 02"))
                    .withClass(new ClassTest("my-structure-02-class-01", "my structure 02 class 01"), "my-structure-02")
                    .withClass(new ClassTest("my-structure-02-class-02", "my structure 02 class 02"), "my-structure-02")
                .withUser(teacher)
                    .teacherInClass(teacher.getId(), "my-structure-01-class-01")
                    .teacherInClass(teacher.getId(), "my-structure-02-class-01")
                    .withUserPosition(TEACHER_USER_POS.toLowerCase(), adml.getId(), "my-structure-01")
                .withUser(adml)
                .adml(adml.getId(), "my-structure-01")
                .withUserPosition(DIRECTOR_USER_POS.toLowerCase(), adml.getId(), "my-structure-01")
                .withStructureLink("my-structure-02", "my-structure-01")
                .withManualGroup(new org.entcore.test.preparation.GroupTest("group-01", "group-01"),
                        "my-structure-01",
                        "BroadcastGroup",
                        Lists.newArrayList(DIRECTOR_USER_POS.toUpperCase()));
        return dataHelper.execute();
    }
}
