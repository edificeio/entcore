package org.entcore.directory.org.entcore.directory.services.impl;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.apache.commons.lang3.StringUtils;
import org.entcore.common.events.EventStoreFactory;
import org.entcore.directory.services.impl.DefaultUserService;
import org.entcore.test.TestHelper;
import org.entcore.test.preparation.ClassTest;
import org.entcore.test.preparation.DataHelper;
import org.entcore.test.preparation.Profile;
import org.entcore.test.preparation.StructureTest;
import org.entcore.test.preparation.UserBookTest;
import org.entcore.test.preparation.UserTest;
import org.entcore.test.preparation.UserTestBuilder;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.testcontainers.containers.Neo4jContainer;

import java.util.Set;
import java.util.stream.Collectors;

@RunWith(VertxUnitRunner.class)
public class DefaultUserServiceTest {
    private static final TestHelper test = TestHelper.helper();
    @ClassRule
    public static Neo4jContainer<?> neo4jContainer = test.database().createNeo4jContainer();

    private static DefaultUserService defaultUserService;

    private static DataHelper dataHelper;
    static final UserTest user0 = UserTestBuilder.anUserTest().id("simple-user")
            .login("simple.user")
            .firstName("prenom").lastName("NdF")
            .displayName("Prenom NdF")
            .build();
    static final UserTest child1 = UserTestBuilder.anUserTest().id("child")
            .login("child")
            .firstName("Enfant").lastName("Child")
            .displayName("Enfant Child")
            .build();
    static final UserTest teacher = UserTestBuilder.anUserTest().id("user.two")
            .login("user-two")
            .firstName("User").lastName("Two")
            .displayName("User Two")
            .profile(Profile.Teacher)
            .userBook(new UserBookTest("user.two", "ine.user.two", 1000, 0))
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
        defaultUserService = new DefaultUserService(
                null,
                vertx.eventBus(),
                new JsonObject()
                        .put("default-avatar", "avatar.png")
                        .put("default-theme", "theme")
                        .put("hobbies", new JsonArray().add("cinema").add("sport")));
        dataHelper = DataHelper.init(context, neo4jContainer);
        prepareData().onComplete(context.asyncAssertSuccess());
    }

    /**
     * <h1>Goal</h1>
     * <p>Ensures that the function returns an empty result when the user does not exist.</p>
     */
    @Test
    public void testGetUserInfosWithUnexistingUser(final TestContext testContext) {
        final Async async = testContext.async();
        defaultUserService.getUserInfos("does-not-exist", h -> {
            testContext.assertTrue(h.isRight(), "Failed to get user infos with unknown user " + h);
            final JsonObject userInfos = h.right().getValue();
            testContext.assertFalse(userInfos.isEmpty(), "Should not have returned data for the user");
            final Set<String> fieldNames = userInfos.fieldNames();
            testContext.assertEquals(1, fieldNames.size(), "Returned object should only contain hobbies but had instead " + fieldNames);
            testContext.assertEquals("hobbies", fieldNames.stream().findAny().orElse(""), "Returned object should only contain hobbies but had instead " + fieldNames);
            async.complete();
        });
    }

    /**
     * <h1>Goal</h1>
     * <p>Ensures that the function returns user's info when the user has no link to a structure or a userbook.</p>
     */
    @Test
    public void testGetUserInfosSimpleUSer(final TestContext testContext) {
        final Async async = testContext.async();
        defaultUserService.getUserInfos(user0.getId(), h -> {
            testContext.assertTrue(h.isRight(), "Failed to get user infos of existing user " + h);
            final JsonObject userInfos = h.right().getValue();
            testContext.assertTrue(!userInfos.containsKey("lockedEmail") || userInfos.getBoolean("lockedEmail"));
            testContext.assertFalse(userInfos.isEmpty(), "Should not have returned data for the user");
            testContext.assertEquals(user0.getId(), userInfos.getString("id"));
            testContext.assertEquals(user0.getFirstName(), userInfos.getString("firstName"));
            testContext.assertEquals(user0.getLastName(), userInfos.getString("lastName"));
            testContext.assertEquals(null, userInfos.getString("profiles"));
            testContext.assertEquals(null, userInfos.getJsonArray("schools").getJsonObject(0).getString("id"));
            async.complete();
        });
    }

    /**
     * <h1>Goal</h1>
     * <p>Ensures that the function returns user's info for a teacher.</p>
     */
    @Test
    public void testGetTeacherUserInfos(final TestContext testContext) {
        final Async async = testContext.async();
        defaultUserService.getUserInfos(teacher.getId(), h -> {
            testContext.assertTrue(h.isRight(), "Failed to get user infos of known user" + h);
            final JsonObject userInfos = h.right().getValue();
            testContext.assertFalse(userInfos.isEmpty(), "Should not have returned data for the user");
            testContext.assertEquals(teacher.getId(), userInfos.getString("id"));
            testContext.assertEquals(teacher.getFirstName(), userInfos.getString("firstName"));
            testContext.assertEquals(teacher.getLastName(), userInfos.getString("lastName"));
            testContext.assertTrue(!userInfos.containsKey("lockedEmail") || userInfos.getBoolean("lockedEmail"));
            final JsonArray profiles = userInfos.getJsonArray("profiles");
            testContext.assertNotNull(profiles, "The user should have profiles");
            testContext.assertEquals(1, profiles.size(), "This user should have only one profile");
            testContext.assertTrue(profiles.contains(teacher.getProfile().name));
            final Set<String> schools = userInfos.getJsonArray("schools").stream()
                    .map(s -> ((JsonObject)s).getString("id"))
                    .filter(StringUtils::isNotBlank)
                    .collect(Collectors.toSet());
            testContext.assertTrue( schools.contains("my-structure-01"), "Should be linked to structure 01 by classes my structure 01 class 01 and my structure 01 class 02 but instead was linked to " + schools);
            testContext.assertTrue( schools.contains("my-structure-02"), "Should be linked to structure 02 by classes my structure 02 class 01 but instead was linked to " + schools);
            final Set<String> classes = userInfos.getJsonArray("schools").stream()
                    .flatMap(s -> ((JsonObject)s).getJsonArray("classes").stream())
                    .map(c -> ((JsonObject) c).getString("id"))
                    .filter(StringUtils::isNotBlank)
                    .collect(Collectors.toSet());
            testContext.assertTrue( classes.contains("my-structure-01-class-01"), "Should be linked to this class because I am a teacher in it. Got instead" + classes);
            testContext.assertTrue( classes.contains("my-structure-02-class-01"), "Should be linked to this class because I am a teacher in it. Got instead" + classes);

            final Set<String> relativesId = userInfos.getJsonArray("relativeList").stream()
                    .map((o -> (JsonObject) o))
                    .map(j -> j.getString("id"))
                    .filter(StringUtils::isNotBlank)
                    .collect(Collectors.toSet());
            testContext.assertTrue(relativesId.isEmpty(), "Should have no relatives but got : " + relativesId);
            async.complete();
        });
    }



    /**
     * <h1>Goal</h1>
     * <p>Ensures that the function returns user's info for a relative.</p>
     */
    @Test
    public void testGetRelativeUserInfos(final TestContext testContext) {
        final Async async = testContext.async();
        defaultUserService.getUserInfos(parent.getId(), h -> {
            testContext.assertTrue(h.isRight(), "Failed to get user infos of known user" + h);
            final JsonObject userInfos = h.right().getValue();
            testContext.assertFalse(userInfos.isEmpty(), "Should not have returned data for the user");
            testContext.assertEquals(parent.getId(), userInfos.getString("id"));
            testContext.assertEquals(parent.getFirstName(), userInfos.getString("firstName"));
            testContext.assertEquals(parent.getLastName(), userInfos.getString("lastName"));
            final JsonArray profiles = userInfos.getJsonArray("profiles");
            testContext.assertNotNull(profiles, "The user should have profiles");
            testContext.assertEquals(1, profiles.size(), "This user should have only one profile");
            testContext.assertTrue(profiles.contains(parent.getProfile().name));
            testContext.assertTrue(!userInfos.containsKey("lockedEmail") || userInfos.getBoolean("lockedEmail"));
            final Set<String> schools = userInfos.getJsonArray("schools").stream()
                    .map(s -> ((JsonObject)s).getString("id"))
                    .filter(StringUtils::isNotBlank)
                    .collect(Collectors.toSet());
            testContext.assertTrue( schools.contains("my-structure-01"), "Should be linked to structure 01 by classes my structure 01 via the child but instead was linked to " + schools);
            final Set<String> classes = userInfos.getJsonArray("schools").stream()
                    .flatMap(s -> ((JsonObject)s).getJsonArray("classes").stream())
                    .map(c -> ((JsonObject) c).getString("id"))
                    .filter(StringUtils::isNotBlank)
                    .collect(Collectors.toSet());
            testContext.assertTrue( classes.contains("my-structure-01-class-02"), "Should be linked to this class because I am a parent of a child in it. Got instead" + classes);

            final Set<String> relativesId = userInfos.getJsonArray("relativeList").stream().map((o -> (JsonObject) o)).map(j -> j.getString("relatedId")).collect(Collectors.toSet());
            testContext.assertEquals(1, relativesId.size(), "Should have one relative which is " + child1.getId());
            testContext.assertTrue(relativesId.contains(child1.getId()), "Should have one relative which is " + child1.getId() + " but got : " + relativesId);
            async.complete();
        });
    }

    /**
     * <h1>Goal</h1>
     * <p>Ensures that the function returns user's info for an ADML.</p>
     */
    @Test
    public void testGetUserAdml(final TestContext testContext) {
        final Async async = testContext.async();
        defaultUserService.getUserInfos(adml.getId(), h -> {
            testContext.assertTrue(h.isRight(), "Failed to get user infos of ADML " + h);
            final JsonObject userInfos = h.right().getValue();
            testContext.assertFalse(userInfos.isEmpty(), "Should not have returned data for the user");
            testContext.assertNotNull(userInfos.getBoolean("lockedEmail"), "As an ADML, lockedEmail should be returned");
            testContext.assertTrue(userInfos.getBoolean("lockedEmail"), "As an ADML, lockedEmail should be true");
            async.complete();
        });
    }

    @Test
    public void testGetUsersDisplayNamesWithEmptyUserId(final TestContext testContext) {
        final Async async = testContext.async();
        defaultUserService.getUsersDisplayNames(new JsonArray())
                .onFailure(testContext::fail)
                .onSuccess(usersDisplayName -> {
                    testContext.assertNotNull(usersDisplayName, "getUsersDisplayNames should not return null");
                    testContext.assertTrue(usersDisplayName.isEmpty(), "should return no display name");
                    async.complete();
                });
    }

    @Test
    public void testGetUsersDisplayNamesWithNonExistingUser(final TestContext testContext) {
        final Async async = testContext.async();
        defaultUserService.getUsersDisplayNames(new JsonArray().add("unknown-user"))
                .onFailure(testContext::fail)
                .onSuccess(usersDisplayName -> {
                    testContext.assertNotNull(usersDisplayName, "getUsersDisplayNames should not return null");
                    testContext.assertTrue(usersDisplayName.isEmpty(), "No display name for unknown-user");
                    async.complete();
                });
    }

    @Test
    public void testGetUsersDisplayNames(final TestContext testContext) {
        final Async async = testContext.async();
        defaultUserService.getUsersDisplayNames(new JsonArray().add("simple-user").add("child").add("user.two"))
                .onFailure(testContext::fail)
                .onSuccess(usersDisplayName -> {
                    testContext.assertNotNull(usersDisplayName, "usersDisplayName should not be null");
                    testContext.assertEquals("Prenom NdF", usersDisplayName.getString("simple-user"), "simple-user display name should be Prenom NdF");
                    testContext.assertEquals("Enfant Child", usersDisplayName.getString("child"), "child display name should be Enfant Child");
                    testContext.assertEquals("User Two", usersDisplayName.getString("user.two"), "user.two display name should be User Two");
                    async.complete();
                });
    }

    public static Future<Void> prepareData() {
        dataHelper
            .start()
            .withStructure(new StructureTest("my-structure-01", "my structure 01"))
                .withClass(new ClassTest("my-structure-01-class-01", "my structure 01 class 01"), "my-structure-01")
                .withClass(new ClassTest("my-structure-01-class-02", "my structure 01 class 02"), "my-structure-01")
            .withStructure(new StructureTest("my-structure-02", "my structure 02"))
                .withClass(new ClassTest("my-structure-02-class-01", "my structure 02 class 01"), "my-structure-02")
                .withClass(new ClassTest("my-structure-02-class-02", "my structure 02 class 02"), "my-structure-02")
            .withUser(user0)
            .withUser(child1)
                .studentInClass(child1.getId(), "my-structure-01-class-02")
            .withUser(teacher)
                .teacherInClass(teacher.getId(), "my-structure-01-class-01")
                .teacherInClass(teacher.getId(), "my-structure-02-class-01")
            .withUser(parent)
                .parentOf(parent.getId(), child1.getId())
            .withUser(adml)
                .adml(adml.getId(), "my-structure-01");
        return dataHelper.execute();
    }

}
