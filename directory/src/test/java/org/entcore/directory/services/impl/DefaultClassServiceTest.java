package org.entcore.directory.services.impl;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
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

import java.util.Set;
import java.util.stream.Collectors;

@RunWith(VertxUnitRunner.class)
public class DefaultClassServiceTest {
    private static final TestHelper test = TestHelper.helper();
    @ClassRule
    public static Neo4jContainer<?> neo4jContainer = test.database().createNeo4jContainer();

    private static DefaultClassService defaultClassService;

    private static DataHelper dataHelper;
    static final UserTest student = UserTestBuilder.anUserTest().id("class-student")
            .login("class.student")
            .firstName("Eleve").lastName("Simple")
            .displayName("Eleve Simple")
            .profile(Profile.Student)
            .build();
    static final UserTest studentFederated = UserTestBuilder.anUserTest().id("class-student-federated")
            .login("class.student.federated")
            .firstName("Eleve").lastName("Federe")
            .displayName("Eleve Federe")
            .profile(Profile.Student)
            .federated(true)
            .build();
    static final UserTest studentOnStructureWithIdp = UserTestBuilder.anUserTest().id("class-student-structure-with-idp")
            .login("class.student.structure.with.idp")
            .firstName("Eleve").lastName("StructureFederee")
            .displayName("Eleve StructureFederee")
            .profile(Profile.Student)
            .build();
    static final UserTest parent = UserTestBuilder.anUserTest().id("class-parent")
            .login("class.parent")
            .firstName("Parent").lastName("Simple")
            .displayName("Parent Simple")
            .profile(Profile.Relative)
            .build();

    @BeforeClass
    public static void setUp(TestContext context) {
        final Vertx vertx = test.vertx();
        defaultClassService = new DefaultClassService(vertx.eventBus());
        dataHelper = DataHelper.init(context, neo4jContainer);
        prepareData().onComplete(context.asyncAssertSuccess());
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
     * <p>Ensures that the function returns the users of the class with federated flag = false when the user is not
     * federated and federated flag = true when federated = true and federatedIDP != null.</p>
     */
    @Test
    public void testFindUsersReturnsFederatedFlag(final TestContext testContext) {
        final Async async = testContext.async();
        defaultClassService.findUsers("class-structure-01-class-01", null, false, false, h -> {
            testContext.assertTrue(h.isRight(), "Failed to find users of class " + h);
            final JsonArray users = h.right().getValue();
            final JsonObject simpleStudent = findById(users, student.getId());
            testContext.assertNotNull(simpleStudent, "Simple student should be in the class");
            testContext.assertEquals(Boolean.FALSE, simpleStudent.getBoolean("hasFederatedIdentity"));
            final JsonObject federatedStudent = findById(users, studentFederated.getId());
            testContext.assertNotNull(federatedStudent, "Federated student should be in the class");
            testContext.assertEquals(Boolean.TRUE, federatedStudent.getBoolean("hasFederatedIdentity"));
            async.complete();
        });
    }

    /**
     * <h1>Goal</h1>
     * <p>Ensures that the function returns the users of the class with federated flag = true when the user is in a
     * structure with a federated auth default.</p>
     */
    @Test
    public void testFindUsersOnFederatedStructure(final TestContext testContext) {
        final Async async = testContext.async();
        defaultClassService.findUsers("class-structure-02-class-01", null, false, false, h -> {
            testContext.assertTrue(h.isRight(), "Failed to find users of class " + h);
            final JsonArray users = h.right().getValue();
            final JsonObject studentOnFederatedStructure = findById(users, studentOnStructureWithIdp.getId());
            testContext.assertNotNull(studentOnFederatedStructure, "Student of the federated structure should be in the class");
            testContext.assertEquals(Boolean.TRUE, studentOnFederatedStructure.getBoolean("hasFederatedIdentity"));
            async.complete();
        });
    }

    /**
     * <h1>Goal</h1>
     * <p>Ensures that the function still returns the relatives and the federated flag when types are filtered and
     * relatives are collected.</p>
     */
    @Test
    public void testFindUsersWithTypesAndCollectRelative(final TestContext testContext) {
        final Async async = testContext.async();
        defaultClassService.findUsers("class-structure-01-class-01", new JsonArray().add(Profile.Student.name), true, false, h -> {
            testContext.assertTrue(h.isRight(), "Failed to find users of class " + h);
            final JsonArray users = h.right().getValue();
            final Set<String> types = users.stream()
                    .map(JsonObject.class::cast)
                    .map(u -> u.getString("type"))
                    .collect(Collectors.toSet());
            testContext.assertEquals(1, types.size(), "Should only contain students but got " + types);
            testContext.assertTrue(types.contains(Profile.Student.name));
            final JsonObject simpleStudent = findById(users, student.getId());
            testContext.assertNotNull(simpleStudent, "Simple student should be in the class");
            testContext.assertEquals(Boolean.FALSE, simpleStudent.getBoolean("hasFederatedIdentity"));
            final Set<String> relativesId = simpleStudent.getJsonArray("relativeList").stream()
                    .map(JsonObject.class::cast)
                    .map(r -> r.getString("relatedId"))
                    .collect(Collectors.toSet());
            testContext.assertTrue(relativesId.contains(parent.getId()), "Should have one relative which is " + parent.getId() + " but got : " + relativesId);
            final JsonObject federatedStudent = findById(users, studentFederated.getId());
            testContext.assertNotNull(federatedStudent, "Federated student should be in the class");
            testContext.assertEquals(Boolean.TRUE, federatedStudent.getBoolean("hasFederatedIdentity"));
            async.complete();
        });
    }

    public static Future<Void> prepareData() {
        dataHelper
            .start()
            .withStructure(new StructureTest("class-structure-01", "class structure 01"))
                .withClass(new ClassTest("class-structure-01-class-01", "class structure 01 class 01"), "class-structure-01")
            .withStructure(new StructureTest("class-structure-02", "class structure 02", true))
                .withClass(new ClassTest("class-structure-02-class-01", "class structure 02 class 01"), "class-structure-02")
            .withUser(student)
                .studentInClass(student.getId(), "class-structure-01-class-01")
            .withUser(studentFederated)
                .studentInClass(studentFederated.getId(), "class-structure-01-class-01")
            .withUser(studentOnStructureWithIdp)
                .studentInClass(studentOnStructureWithIdp.getId(), "class-structure-02-class-01")
            .withUser(parent)
                .parentOf(parent.getId(), student.getId());
        return dataHelper.execute();
    }

}