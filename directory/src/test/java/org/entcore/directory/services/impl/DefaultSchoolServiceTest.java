package org.entcore.directory.services.impl;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.entcore.common.events.EventStoreFactory;
import org.entcore.common.neo4j.Neo4j;
import org.entcore.test.TestHelper;
import org.entcore.test.preparation.*;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.testcontainers.containers.Neo4jContainer;

import java.util.Optional;

@RunWith(VertxUnitRunner.class)
public class DefaultSchoolServiceTest {

    private static final TestHelper test = TestHelper.helper();
    private static DataHelper dataHelper;
    @ClassRule
    public static Neo4jContainer<?> neo4jContainer = test.database().createNeo4jContainer();
    private static Neo4j neo4j;
    private static DefaultSchoolService defaultSchoolService;

    static final UserTest teacher = UserTestBuilder.anUserTest().id("teacher.one")
            .login("teacher-one")
            .firstName("teacher").lastName("One")
            .displayName("Teacher One")
            .profile(Profile.Teacher)
            .build();
    static final UserTest teacher2 = UserTestBuilder.anUserTest().id("teacher.two")
            .login("teacher-two")
            .firstName("teacher").lastName("Two")
            .displayName("Teacher Two")
            .profile(Profile.Teacher)
            .build();
    static final UserTest parent = UserTestBuilder.anUserTest().id("user.three")
            .login("user-three")
            .firstName("User").lastName("three")
            .profile(Profile.Relative)
            .userBook(new UserBookTest("user.three", "ine.user.three", 1000, 0)).build();

    static final UserTest adml = UserTestBuilder.anUserTest().id("user.adml")
            .login("user-adml")
            .firstName("Adée").lastName("Émelle")
            .profile(Profile.Personnel)
            .userBook(new UserBookTest("user.adml", "ine.user.adml", 1000, 0)).build();

    @BeforeClass
    public static void setUp(TestContext context) {
        final Vertx vertx = test.vertx();
        EventStoreFactory.getFactory().setVertx(vertx);
        defaultSchoolService = new DefaultSchoolService(vertx.eventBus());
        dataHelper = DataHelper.init(context, neo4jContainer);
        neo4j = Neo4j.getInstance();
        prepareData().onComplete(context.asyncAssertSuccess());
    }

    /**
     * Test pour vérifier que l'on retrouve le duplicat de teacher1 vs teacher2 dans le cas d'un utilisateur rataché
     * @param testContext
     */
    @Test
    public void testGetUserList_duplicate(final TestContext testContext) {
        final Async async = testContext.async();
        defaultSchoolService.userList("my-structure-01", false, true, h ->
        {
            JsonArray usersArray = h.right().getValue();
            testContext.assertNotNull(usersArray,"We should retrieve a user array");
            testContext.assertEquals(usersArray.size(), 3,"We should retrieve 3 user");
            Optional<JsonObject> oTeacher1 = usersArray.stream()
                                            .map(JsonObject.class::cast)
                                            .filter(o -> o.getString("id").equals("teacher.one"))
                                            .findAny();
            testContext.assertTrue(oTeacher1.isPresent(), "Teacher one must be in result list");
            JsonArray duplicates = oTeacher1.get().getJsonArray("duplicates");
            testContext.assertEquals(duplicates.size(), 1, "Teacher one must have one duplicate");
            testContext.assertEquals(duplicates.getJsonObject(0).getString("id"), "teacher.two",
                    "Teacher one must have teacher two in duplicate");
            async.complete();
        });
    }

    private static Future<Void> prepareData() {
        return dataHelper.start()
                .withStructure(new StructureTest("my-structure-01", "my structure 01"))
                    .withClass(new ClassTest("my-structure-01-class-01", "my structure 01 class 01"), "my-structure-01")
                    .withClass(new ClassTest("my-structure-01-class-02", "my structure 01 class 02"), "my-structure-01")
                .withUser(teacher)
                    .teacherInClass(teacher.getId(), "my-structure-01-class-01")
                .withUser(teacher2)
                    .teacherInClass(teacher2.getId(), "my-structure-01-class-01")
                    .duplicate(teacher, teacher2, 4)
                .withUser(parent)
                .withUser(adml)
                    .adml(adml.getId(), "my-structure-01")
                .execute();
    }
}
