package org.entcore.directory.services.impl;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.entcore.common.events.EventStoreFactory;
import org.entcore.common.neo4j.Neo4j;
import org.entcore.common.user.dto.TimezonePreference;
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

    // -----------------------------------------------------------------------
    //  Helper methods for quiet hours / timezone preference tests
    // -----------------------------------------------------------------------

    private Future<JsonObject> setQuietHoursPreferences(String structureId, JsonObject body) {
        final Promise<JsonObject> promise = Promise.promise();
        defaultSchoolService.setQuietHoursPreferences(structureId, body, result -> {
            if (result.isRight()) {
                promise.complete(result.right().getValue());
            } else {
                promise.fail(result.left().getValue());
            }
        });
        return promise.future();
    }

    private Future<JsonObject> getQuietHoursPreferences(String structureId) {
        final Promise<JsonObject> promise = Promise.promise();
        defaultSchoolService.getQuietHoursPreferences(structureId, result -> {
            if (result.isRight()) {
                promise.complete(result.right().getValue());
            } else {
                promise.fail(result.left().getValue());
            }
        });
        return promise.future();
    }

    // -----------------------------------------------------------------------
    //  Timezone preservation tests
    // -----------------------------------------------------------------------

    @Test
    public void testSetQuietHoursPreferences_timezoneAbsent_preservesExisting(final TestContext context) {
        final Async async = context.async();
        final String structureId = "my-structure-01";

        final JsonObject initialBody = new JsonObject()
                .put("timezone", "Europe/Paris")
                .put("quietHours", new JsonObject()
                        .put("schedule", new JsonArray())
                        .put("enabled", true));

        // Update without any "timezone" key at all
        final JsonObject updateBody = new JsonObject()
                .put("quietHours", new JsonObject()
                        .put("schedule", new JsonArray())
                        .put("enabled", true));

        setQuietHoursPreferences(structureId, initialBody)
                .compose(ignored -> setQuietHoursPreferences(structureId, updateBody))
                .compose(ignored -> getQuietHoursPreferences(structureId))
                .onComplete(asyncResult -> {
                    if (asyncResult.succeeded()) {
                        final JsonObject preferences = asyncResult.result();
                        final String timezoneJson = preferences.getString("notificationTimezone");
                        context.assertNotNull(timezoneJson, "timezone should be preserved when absent from request");
                        final TimezonePreference timezone = Json.decodeValue(timezoneJson, TimezonePreference.class);
                        context.assertEquals("Europe/Paris", timezone.getTimezone());
                    } else {
                        context.fail(asyncResult.cause());
                    }
                    async.complete();
                });
    }



    @Test
    public void testSetQuietHoursPreferences_explicitTimezoneUpdate_works(final TestContext context) {
        final Async async = context.async();
        final String structureId = "my-structure-01";

        final JsonObject initialBody = new JsonObject()
                .put("timezone", "Europe/Paris")
                .put("quietHours", new JsonObject()
                        .put("schedule", new JsonArray())
                        .put("enabled", true));

        // Update with a different explicit timezone
        final JsonObject updateBody = new JsonObject()
                .put("timezone", "America/Chicago")
                .put("quietHours", new JsonObject()
                        .put("schedule", new JsonArray())
                        .put("enabled", true));

        setQuietHoursPreferences(structureId, initialBody)
                .compose(ignored -> setQuietHoursPreferences(structureId, updateBody))
                .compose(ignored -> getQuietHoursPreferences(structureId))
                .onComplete(asyncResult -> {
                    if (asyncResult.succeeded()) {
                        final JsonObject preferences = asyncResult.result();
                        final String timezoneJson = preferences.getString("notificationTimezone");
                        context.assertNotNull(timezoneJson);
                        final TimezonePreference timezone = Json.decodeValue(timezoneJson, TimezonePreference.class);
                        context.assertEquals("America/Chicago", timezone.getTimezone(),
                                "explicit timezone update should replace previous value");
                    } else {
                        context.fail(asyncResult.cause());
                    }
                    async.complete();
                });
    }

    // -----------------------------------------------------------------------
    //  Schedule validation hardening tests
    // -----------------------------------------------------------------------

    @Test
    public void testSetQuietHoursPreferences_invalidScheduleTooManyDays_rejected(final TestContext context) {
        final Async async = context.async();
        final String structureId = "my-structure-01";

        // More than 7 days is invalid
        final JsonArray badSchedule = new JsonArray();
        for (int dayCounter = 0; dayCounter < 8; dayCounter++) {
            badSchedule.add(new JsonArray());
        }

        final JsonObject body = new JsonObject()
                .put("quietHours", new JsonObject()
                        .put("schedule", badSchedule)
                        .put("enabled", true));

        defaultSchoolService.setQuietHoursPreferences(structureId, body, result -> {
            context.assertTrue(result.isLeft(), "schedule with >7 days should be rejected");
            context.assertEquals("invalid.preference.data", result.left().getValue());
            async.complete();
        });
    }

    @Test
    public void testSetQuietHoursPreferences_invalidScheduleNonInteger_rejected(final TestContext context) {
        final Async async = context.async();
        final String structureId = "my-structure-01";

        // 7 days but with non-integer hour values
        final JsonArray badSchedule = new JsonArray();
        for (int dayCounter = 0; dayCounter < 7; dayCounter++) {
            badSchedule.add(new JsonArray().add("not-a-number"));
        }

        final JsonObject body = new JsonObject()
                .put("quietHours", new JsonObject()
                        .put("schedule", badSchedule)
                        .put("enabled", true));

        defaultSchoolService.setQuietHoursPreferences(structureId, body, result -> {
            context.assertTrue(result.isLeft(), "schedule with non-integer values should be rejected");
            context.assertEquals("invalid.preference.data", result.left().getValue());
            async.complete();
        });
    }

    // -----------------------------------------------------------------------
    //  Enabled validation tests
    // -----------------------------------------------------------------------

    @Test
    public void testSetQuietHoursPreferences_enabledAbsent_rejected(final TestContext context) {
        final Async async = context.async();
        final String structureId = "my-structure-01";

        final JsonObject body = new JsonObject()
                .put("timezone", "Europe/Paris")
                .put("quietHours", new JsonObject()
                        .put("schedule", new JsonArray()));
        // No "enabled" in quietHours

        defaultSchoolService.setQuietHoursPreferences(structureId, body, result -> {
            context.assertTrue(result.isLeft(), "missing 'enabled' should be rejected");
            context.assertEquals("invalid.preference.data", result.left().getValue());
            async.complete();
        });
    }

    @Test
    public void testSetQuietHoursPreferences_enabledFalse_emptySchedule_accepted(final TestContext context) {
        final Async async = context.async();
        final String structureId = "my-structure-01";

        final JsonObject body = new JsonObject()
                .put("timezone", "Europe/Paris")
                .put("quietHours", new JsonObject()
                        .put("schedule", new JsonArray())
                        .put("enabled", false));

        defaultSchoolService.setQuietHoursPreferences(structureId, body, result -> {
            context.assertTrue(result.isRight(), "enabled=false with empty schedule should be accepted");
            async.complete();
        });
    }

    @Test
    public void testSetQuietHoursPreferences_enabledTrue_emptySchedule_rejected(final TestContext context) {
        final Async async = context.async();
        final String structureId = "my-structure-01";

        final JsonObject body = new JsonObject()
                .put("timezone", "Europe/Paris")
                .put("quietHours", new JsonObject()
                        .put("schedule", new JsonArray())
                        .put("enabled", true));

        defaultSchoolService.setQuietHoursPreferences(structureId, body, result -> {
            // validate() returns false when enabled=true and schedule.length==0
            context.assertTrue(result.isLeft(), "enabled=true with empty schedule should be rejected");
            context.assertEquals("invalid.preference.data", result.left().getValue());
            async.complete();
        });
    }
}
