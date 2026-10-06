package org.entcore.directory.services.impl;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.entcore.common.neo4j.Neo4j;
import org.entcore.test.TestHelper;
import org.entcore.test.preparation.ClassTest;
import org.entcore.test.preparation.DataHelper;
import org.entcore.test.preparation.Profile;
import org.entcore.test.preparation.StructureTest;
import org.entcore.test.preparation.UserTest;
import org.entcore.test.preparation.UserTestBuilder;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.testcontainers.containers.Neo4jContainer;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
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
            .activationCode("class-student-structure-with-idp-activation-code")
            .build();
    static final UserTest detached = UserTestBuilder.anUserTest().id("class-detached")
            .login("class.detached")
            .firstName("Personnel").lastName("Detache")
            .displayName("Personnel Detache")
            .profile(Profile.Personnel)
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

    /**
     * <h1>Goal</h1>
     * <p>Ensures that the members of a class are its students and teachers, whether the class is given or taken
     * from the classes of the user.</p>
     */
    @Test
    public void testListClassMembers(final TestContext testContext) {
        final Set<String> expected = new HashSet<>(Arrays.asList(student.getId(), studentFederated.getId()));
        defaultClassService.listClassMembers(null, "class-structure-01-class-01")
                .compose(byClass -> {
                    testContext.assertEquals(expected, ids(byClass), "Students and teachers only, relatives excluded");
                    return defaultClassService.listClassMembers(student.getId(), null);
                })
                .onComplete(testContext.asyncAssertSuccess(byUser ->
                        testContext.assertEquals(expected, ids(byUser), "Members of the classes of the user")));
    }

    /**
     * <h1>Goal</h1>
     * <p>Ensures that every member is kept, and that the userbook details are dropped for the members the user
     * cannot see.</p>
     */
    @Test
    public void testMarkVisibles() {
        final JsonArray members = new JsonArray()
                .add(new JsonObject().put("type", "Student").put("id", "visible").put("displayName", "Visible")
                        .put("mood", "happy").put("userId", "visible").put("photo", "/photo/visible"))
                .add(new JsonObject().put("type", "Student").put("id", "hidden").put("displayName", "Hidden")
                        .put("mood", "sad").put("userId", "hidden").put("photo", "/photo/hidden"));

        final JsonArray marked = DefaultClassService.markVisibles(members, Collections.singleton("visible"));

        Assert.assertEquals(new JsonArray()
                .add(new JsonObject().put("type", "Student").put("id", "visible").put("displayName", "Visible")
                        .put("mood", "happy").put("userId", "visible").put("photo", "/photo/visible").put("isVisible", true))
                .add(new JsonObject().put("type", "Student").put("id", "hidden").put("displayName", "Hidden")
                        .put("isVisible", false)), marked);
    }

    /**
     * <h1>Goal</h1>
     * <p>Ensures that the users of a class are listed whatever their profile, with their relatives only when
     * asked.</p>
     */
    @Test
    public void testListClassUsers(final TestContext testContext) {
        final Set<String> expected = new HashSet<>(Arrays.asList(student.getId(), studentFederated.getId(), parent.getId()));
        defaultClassService.listClassUsers("class-structure-01-class-01", true)
                .compose(withRelatives -> {
                    testContext.assertEquals(expected, ids(withRelatives), "Every profile of the class, relatives included");
                    testContext.assertEquals(Profile.Relative.name, findById(withRelatives, parent.getId()).getString("type"));
                    final JsonArray relatives = findById(withRelatives, student.getId()).getJsonArray("relativeList");
                    testContext.assertEquals(1, relatives.size(), "The student has one relative");
                    testContext.assertEquals(parent.getId(), relatives.getJsonObject(0).getString("relatedId"));
                    return defaultClassService.listClassUsers("class-structure-01-class-01", false);
                })
                .onComplete(testContext.asyncAssertSuccess(withoutRelatives -> {
                    testContext.assertEquals(expected, ids(withoutRelatives));
                    testContext.assertTrue(findById(withoutRelatives, student.getId()).getJsonArray("relativeList").isEmpty(),
                            "Relatives are not collected unless asked");
                }));
    }

    /**
     * <h1>Goal</h1>
     * <p>Ensures that the detached candidates are the users of the structures attached to none of their classes,
     * and that the login is not returned.</p>
     */
    @Test
    public void testListDetachedCandidates(final TestContext testContext) {
        defaultClassService.listDetachedCandidates(new JsonArray().add("class-structure-01"))
                .onComplete(testContext.asyncAssertSuccess(candidates -> {
                    testContext.assertEquals(Collections.singleton(detached.getId()), ids(candidates),
                            "Only the user without class, got " + candidates.encode());
                    final JsonObject candidate = candidates.getJsonObject(0);
                    testContext.assertEquals(Profile.Personnel.name, candidate.getString("type"));
                    testContext.assertEquals("class-structure-01", candidate.getString("structureId"));
                    testContext.assertFalse(candidate.containsKey("login"), "The login must not be returned");
                }));
    }

    /**
     * <h1>Goal</h1>
     * <p>Ensures that attaching a student to a class puts him/her in the student group of the class and his/her
     * relatives in the relative group, and gives back the structure of the class.</p>
     */
    @Test
    public void testAttachStudentToClass(final TestContext testContext) {
        final String classId = "class-structure-02-class-01";
        final String linkRelative =
                "MERGE (s:User {id: 'attach-student', profiles: ['Student'], displayName: 'Attach Student'}) " +
                "MERGE (r:User {id: 'attach-relative', profiles: ['Relative'], displayName: 'Attach Relative'}) " +
                "MERGE (s)-[:RELATED]->(r)";
        final Async async = testContext.async();
        Neo4j.getInstance().execute(linkRelative, new JsonObject(), e ->
                defaultClassService.attachToClass(classId, "attach-student", Profile.Student.name, attached -> {
                    testContext.assertTrue(attached.isRight(), "Failed to attach " + attached);
                    testContext.assertEquals("attach-student", attached.right().getValue().getString("id"));
                    testContext.assertEquals("class-structure-02", attached.right().getValue().getString("schoolId"));
                    defaultClassService.listClassUsers(classId, false).onComplete(testContext.asyncAssertSuccess(users -> {
                        testContext.assertEquals(Profile.Student.name, findById(users, "attach-student").getString("type"));
                        testContext.assertEquals(Profile.Relative.name, findById(users, "attach-relative").getString("type"));
                        async.complete();
                    }));
                }));
    }

    /**
     * <h1>Goal</h1>
     * <p>Ensures that attaching a user to a class without a group for his/her profile is refused, as before.</p>
     */
    @Test
    public void testAttachToClassWithoutProfileGroup(final TestContext testContext) {
        final Async async = testContext.async();
        defaultClassService.attachToClass("class-structure-02-class-01", student.getId(), "UnknownProfile", attached -> {
            testContext.assertTrue(attached.isLeft());
            testContext.assertEquals("user.not.visible", attached.left().getValue());
            async.complete();
        });
    }

    private static Set<String> ids(final JsonArray users) {
        return users.stream().map(o -> ((JsonObject) o).getString("id")).collect(Collectors.toSet());
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
                .parentOf(parent.getId(), student.getId())
            .withUser(detached)
                .adml(detached.getId(), "class-structure-01");
        return dataHelper.execute();
    }

}