package org.entcore.directory.services.impl;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.entcore.common.user.DefaultFunctions;
import org.entcore.common.user.UserInfos;
import org.entcore.test.TestHelper;
import org.entcore.test.preparation.ClassTest;
import org.entcore.test.preparation.DataHelper;
import org.entcore.test.preparation.Profile;
import org.entcore.test.preparation.StructureTest;
import org.entcore.test.preparation.UserTest;
import org.entcore.test.preparation.UserTestBuilder;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.testcontainers.containers.Neo4jContainer;
import org.testcontainers.shaded.com.google.common.collect.ImmutableMap;

@RunWith(VertxUnitRunner.class)
public class DefaultMassMailServiceTest {
    private static final TestHelper test = TestHelper.helper();
    @ClassRule
    public static Neo4jContainer<?> neo4jContainer = test.database().createNeo4jContainer();

    private static DefaultMassMailService defaultMassMailService;

    private static DataHelper dataHelper;
    static final UserTest student = UserTestBuilder.anUserTest().id("massmail-student")
            .login("massmail.student")
            .firstName("Eleve").lastName("Simple")
            .displayName("Eleve Simple")
            .profile(Profile.Student)
            .build();
    static final UserTest studentFederated = UserTestBuilder.anUserTest().id("massmail-student-federated")
            .login("massmail.student.federated")
            .firstName("Eleve").lastName("Federe")
            .displayName("Eleve Federe")
            .profile(Profile.Student)
            .federated(true)
            .build();
    static final UserTest studentOnStructureWithIdp = UserTestBuilder.anUserTest().id("massmail-student-structure-with-idp")
            .login("massmail.student.structure.with.idp")
            .firstName("Eleve").lastName("StructureFederee")
            .displayName("Eleve StructureFederee")
            .profile(Profile.Student)
            .activationCode("massmail-student-structure-with-idp-activation-code")
            .build();

    @BeforeClass
    public static void setUp(TestContext context) {
        final Vertx vertx = test.vertx();
        defaultMassMailService = new DefaultMassMailService(vertx, vertx.eventBus(), null, new JsonObject(), null);
        dataHelper = DataHelper.init(context, neo4jContainer);
        prepareData().onComplete(context.asyncAssertSuccess());
    }

    private static UserInfos superAdmin() {
        final UserInfos userInfos = new UserInfos();
        userInfos.setFunctions(ImmutableMap.of(DefaultFunctions.SUPER_ADMIN, new UserInfos.Function()));
        return userInfos;
    }

    private static JsonObject findById(final JsonArray users, final String id) {
        return users.stream()
                .map(JsonObject.class::cast)
                .filter(u -> id.equals(u.getString("id")))
                .findFirst()
                .orElse(null);
    }

    /**
     * <h1>Goal</h1>
     * <p>Ensures that massmailUsers, when "includeFederated" is set, returns the users of the structure with
     * federated flag = false when the user is not federated and federated flag = true when federated = true and
     * federatedIDP != null.</p>
     */
    @Test
    public void testMassmailUsersReturnsFederatedFlag(final TestContext testContext) {
        final Async async = testContext.async();
        defaultMassMailService.massmailUsers("massmail-structure-01", new JsonObject().put("activated", "all").put("includeFederated", true), superAdmin(), h -> {
            testContext.assertTrue(h.isRight(), "Failed to find users of structure " + h);
            final JsonArray users = h.right().getValue();
            final JsonObject simpleStudent = findById(users, student.getId());
            testContext.assertNotNull(simpleStudent, "Simple student should be in the structure");
            testContext.assertEquals(Boolean.FALSE, simpleStudent.getBoolean("hasFederatedIdentity"));
            final JsonObject federatedStudent = findById(users, studentFederated.getId());
            testContext.assertNotNull(federatedStudent, "Federated student should be in the structure");
            testContext.assertEquals(Boolean.TRUE, federatedStudent.getBoolean("hasFederatedIdentity"));
            async.complete();
        });
    }

    /**
     * <h1>Goal</h1>
     * <p>Ensures that massmailUsers, when "includeFederated" is set, returns the users with federated flag = true
     * when the user is in a structure with a federated auth default.</p>
     */
    @Test
    public void testMassmailUsersOnFederatedStructure(final TestContext testContext) {
        final Async async = testContext.async();
        defaultMassMailService.massmailUsers("massmail-structure-02", new JsonObject().put("activated", "all").put("includeFederated", true), superAdmin(), h -> {
            testContext.assertTrue(h.isRight(), "Failed to find users of structure " + h);
            final JsonArray users = h.right().getValue();
            final JsonObject studentOnFederatedStructure = findById(users, studentOnStructureWithIdp.getId());
            testContext.assertNotNull(studentOnFederatedStructure, "Student of the federated structure should be in the structure");
            testContext.assertEquals(Boolean.TRUE, studentOnFederatedStructure.getBoolean("hasFederatedIdentity"));
            async.complete();
        });
    }

    /**
     * <h1>Goal</h1>
     * <p>Ensures that massmailUsers excludes federated users (federated identity) by default, i.e. when the
     * "includeFederated" filter is not set, while still returning non-federated users.</p>
     */
    @Test
    public void testMassmailUsersExcludesFederatedUsersByDefault(final TestContext testContext) {
        final Async async = testContext.async();
        defaultMassMailService.massmailUsers("massmail-structure-01", new JsonObject().put("activated", "all"), superAdmin(), h -> {
            testContext.assertTrue(h.isRight(), "Failed to find users of structure " + h);
            final JsonArray users = h.right().getValue();
            testContext.assertNotNull(findById(users, student.getId()), "Simple student should be in the structure");
            testContext.assertNull(findById(users, studentFederated.getId()), "Federated student should be excluded by default");
            async.complete();
        });
    }

    /**
     * <h1>Goal</h1>
     * <p>Ensures that massmailUsers excludes users belonging to a structure with a federated auth default by
     * default, i.e. when the "includeFederated" filter is not set.</p>
     */
    @Test
    public void testMassmailUsersExcludesFederatedStructureUsersByDefault(final TestContext testContext) {
        final Async async = testContext.async();
        defaultMassMailService.massmailUsers("massmail-structure-02", new JsonObject().put("activated", "all"), superAdmin(), h -> {
            testContext.assertTrue(h.isRight(), "Failed to find users of structure " + h);
            final JsonArray users = h.right().getValue();
            testContext.assertNull(findById(users, studentOnStructureWithIdp.getId()), "Student of the federated structure should be excluded by default");
            async.complete();
        });
    }

    /**
     * <h1>Goal</h1>
     * <p>Ensures that massmailUsers, when the "classes" filter is set, still returns the users of the filtered
     * class with their class fields. This filter builds a different Neo4j query (see the //Classes section of
     * massmailUsers), which has already been broken twice without any test noticing.</p>
     */
    @Test
    public void testMassmailUsersWithClassFilter(final TestContext testContext) {
        final Async async = testContext.async();
        final JsonObject filter = new JsonObject()
                .put("activated", "all")
                .put("profiles", new JsonArray().add(Profile.Student.name))
                .put("classes", new JsonArray().add("massmail-structure-01-class-01"));
        defaultMassMailService.massmailUsers("massmail-structure-01", filter, superAdmin(), h -> {
            testContext.assertTrue(h.isRight(), "Failed to find users of the filtered class " + h);
            final JsonArray users = h.right().getValue();
            final JsonObject simpleStudent = findById(users, student.getId());
            testContext.assertNotNull(simpleStudent, "Simple student should be in the filtered class");
            testContext.assertEquals(new JsonArray().add("massmail structure 01 class 01"), simpleStudent.getJsonArray("classes"));
            testContext.assertEquals("massmail structure 01 class 01", simpleStudent.getString("firstClass"));
            testContext.assertEquals(Boolean.TRUE, simpleStudent.getBoolean("isInClass"));
            testContext.assertEquals(Boolean.FALSE, simpleStudent.getBoolean("hasFederatedIdentity"));
            async.complete();
        });
    }

    /**
     * <h1>Goal</h1>
     * <p>Ensures that massMailAllUsersByStructure returns the users of the structure with federated flag = false
     * when the user is not federated and federated flag = true when federated = true and federatedIDP != null.</p>
     */
    @Test
    public void testMassMailAllUsersByStructureReturnsFederatedFlag(final TestContext testContext) {
        final Async async = testContext.async();
        defaultMassMailService.massMailAllUsersByStructure("massmail-structure-01", superAdmin(), h -> {
            testContext.assertTrue(h.isRight(), "Failed to find users of structure " + h);
            final JsonArray users = h.right().getValue();
            final JsonObject simpleStudent = findById(users, student.getId());
            testContext.assertNotNull(simpleStudent, "Simple student should be in the structure");
            testContext.assertEquals(Boolean.FALSE, simpleStudent.getBoolean("hasFederatedIdentity"));
            final JsonObject federatedStudent = findById(users, studentFederated.getId());
            testContext.assertNotNull(federatedStudent, "Federated student should be in the structure");
            testContext.assertEquals(Boolean.TRUE, federatedStudent.getBoolean("hasFederatedIdentity"));
            async.complete();
        });
    }

    /**
     * <h1>Goal</h1>
     * <p>Ensures that massMailAllUsersByStructure returns the users with federated flag = true when the user is in
     * a structure with a federated auth default.</p>
     */
    @Test
    public void testMassMailAllUsersByStructureOnFederatedStructure(final TestContext testContext) {
        final Async async = testContext.async();
        defaultMassMailService.massMailAllUsersByStructure("massmail-structure-02", superAdmin(), h -> {
            testContext.assertTrue(h.isRight(), "Failed to find users of structure " + h);
            final JsonArray users = h.right().getValue();
            final JsonObject studentOnFederatedStructure = findById(users, studentOnStructureWithIdp.getId());
            testContext.assertNotNull(studentOnFederatedStructure, "Student of the federated structure should be in the structure");
            testContext.assertEquals(Boolean.TRUE, studentOnFederatedStructure.getBoolean("hasFederatedIdentity"));
            async.complete();
        });
    }

    public static Future<Void> prepareData() {
        dataHelper
            .start()
            .withStructure(new StructureTest("massmail-structure-01", "massmail structure 01"))
                .withClass(new ClassTest("massmail-structure-01-class-01", "massmail structure 01 class 01"), "massmail-structure-01")
            .withStructure(new StructureTest("massmail-structure-02", "massmail structure 02", true))
                .withClass(new ClassTest("massmail-structure-02-class-01", "massmail structure 02 class 01"), "massmail-structure-02")
            .withUser(student)
                .studentInClass(student.getId(), "massmail-structure-01-class-01")
            .withUser(studentFederated)
                .studentInClass(studentFederated.getId(), "massmail-structure-01-class-01")
            .withUser(studentOnStructureWithIdp)
                .studentInClass(studentOnStructureWithIdp.getId(), "massmail-structure-02-class-01");
        return dataHelper.execute();
    }

}