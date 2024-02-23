package org.entcore.test.preparation;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import static io.vertx.core.impl.ConversionHelper.toJsonArray;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.TestContext;
import org.entcore.common.neo4j.Neo4j;
import org.entcore.common.neo4j.StatementsBuilder;
import org.entcore.test.TestHelper;
import org.testcontainers.containers.Neo4jContainer;

import java.util.Arrays;
import java.util.UUID;

/**
 * <p>
 * Fluent API to prepare data for unit tests.
 * </p>
 * <p>
 *     <u>Example:</u>
 *     <pre>{@code @RunWith(VertxUnitRunner.class)
public class DefaultUserServiceTest {
    private static final TestHelper test = TestHelper.helper();
    @ClassRule
    public static Neo4jContainer<?> neo4jContainer = test.database().createNeo4jContainer();

    private static DataHelper dataHelper;
...

    public static void setUp(TestContext context) throws Exception {
        final Vertx vertx = test.vertx();
        dataHelper = DataHelper.init(context, neo4jContainer);
...
        prepareData().onComplete(context.asyncAssertSuccess());
    }

}
}</pre>
 * </p>
 */
public class DataHelper {

    private StatementsBuilder sb;
    private static Neo4j neo4j;

    private static final TestHelper test = TestHelper.helper();

    public DataHelper() {
        this.sb = new StatementsBuilder();
    }

    /**
     * To be called in the @BeforeClass of your unit test class.
     * @param context Context of the unit test
     * @param neo4jContainer Neo4J Container
     * @return A helper
     */
    public static DataHelper init(final TestContext context, final Neo4jContainer<?> neo4jContainer) {
        test.database().initNeo4j(context, neo4jContainer);
        final String base = neo4jContainer.getHttpUrl() + "/db/data/";
        final JsonObject neo4jConfig = new JsonObject()
                .put("server-uri", base).put("poolSize", 1);
        neo4j = Neo4j.getInstance();
        neo4j.init(test.vertx(), neo4jConfig
                .put("server-uri", base)
                .put("ignore-empty-statements-error", false));
        return new DataHelper();
    }

    /**
     * Initialize basic Profile nodes.
     * @return
     */
    public DataHelper start() {
        for (final Profile profile : Profile.values()) {
            sb.add("MERGE (p:Profile{id: {id}, externalId: {externalId}, name: {name}})",
                    new JsonObject()
                            .put("id", profile.id)
                            .put("externalId", profile.externalId)
                            .put("name", profile.name));
        }
        return this;
    }

    /**
     * A structure :
     * <ul>
     *     <li>Has a ProfileGroup for each Profile</li>
     *     <li>Has an automatic admin local function</li>
     *     <li>Has an automatic admin local function group</li>
     * </ul>
     * @param structure
     * @return
     */
    public DataHelper withStructure(final StructureTest structure) {
        sb.add("CREATE (:Structure{id: {id}, name: {name}})", new JsonObject()
            .put("id", structure.getId())
            .put("name", structure.getName()));
        for (final Profile profile : Profile.values()) {
            sb.add("MATCH (s:Structure{id: {id}}), (p:Profile{id: {profileId}})" +
                        " MERGE (s)<-[:DEPENDS]-(spg:Group:ProfileGroup{filter: {spgFilter}, name: {name}})-[:HAS_PROFILE]->(p)",
                    new JsonObject()
                            .put("id", structure.getId())
                            .put("profileId", profile.id)
                            .put("spgFilter", profile.name)
                            .put("name", profile.name + "s of " + structure.getName()));
        }
        sb.add("MATCH (s:Structure{id: {id}})<-[:DEPENDS]-(spg:ProfileGroup{filter: {personnelFilter}})-[:HAS_PROFILE]->(p:Profile) " +
                      "MERGE (p)<-[:COMPOSE]-(:Function{externalId: 'ADMIN_LOCAL', id: {functionId}, name: {functionName}}) " +
                      "MERGE (s)<-[:DEPENDS]-(:Group:Visible:FunctionGroup{externalId: {fgExtId}, filter: {functionName}, id: {fgId}, name: {fgName}})",
                new JsonObject()
                        .put("id", structure.getId())
                        .put("profileId", Profile.Personnel.id)
                        .put("personnelFilter", Profile.Personnel.name())
                        .put("functionId", structure.getId() + "-function-adminlocal")
                        .put("functionName", Function.AdminLocal.name())
                        .put("fgExtId", UUID.randomUUID().toString())
                        .put("fgName", structure.getName() + "-AdminLocal")
                        .put("fgId", UUID.randomUUID().toString()));
        return this;
    }

    /**
     * Just insert a node User possibly linked to a UserBook node.
     * @param user User to insert
     * @return This helper
     */
    public DataHelper withUser(final UserTest user) {
        sb.add("CREATE (u:User{id: {id}, login: {login}, lastName:{lastName}, firstName: {firstName}, displayName: {displayName}, profiles: {profiles}})",
                new JsonObject()
                        .put("id", user.getId())
                        .put("login", user.getLogin())
                        .put("firstName", user.getFirstName())
                        .put("lastName", user.getLastName())
                        .put("displayName", user.getDisplayName())
                        .put("profiles", user.getProfile() == null ? null : new JsonArray().add(user.getProfile().name)));
        if(user.getUserBook() != null) {
            final UserBookTest ub = user.getUserBook();
            sb.add("MATCH (u:User{id: {id}}) CREATE (u)-[:USERBOOK]->(:UserBook{userid: {id}, ine: {ine} })", new JsonObject()
                    .put("id", user.getId())
                    .put("ine", ub.getIne())
                    .put("quota", ub.getQuota())
                    .put("storage", ub.getStorage()));
        }
        return this;
    }

    /**
     * A class is :
     * <ul>
     *     <li>a node :Class...</li>
     *     <li>which BELONGS to a structure</li>
     *     <li>has a set of Profile Group for each profile</li>
     * </ul>
     * @param clazz Information about the class to add
     * @param structureId Structure to which the class belongs
     * @return This helper
     */
    public DataHelper withClass(final ClassTest clazz, final String structureId) {
        final String classId = clazz.getId();
        sb.add("MATCH (s:Structure{id: {structId}}) MERGE (s)<-[:BELONGS]-(c:Class{id: {classId}, name: {className}})",
                new JsonObject()
                        .put("structId", structureId)
                        .put("classId", clazz.getId())
                        .put("className", clazz.getName()));
        withClassGroup(new GroupTest(idOfParentClassGroup(classId), "Groupe de parents de " + classId), classId, Profile.Relative);
        withClassGroup(new GroupTest(idOfTeacherClassGroup(classId), "Groupe d'enseignants de " + classId), classId, Profile.Teacher);
        withClassGroup(new GroupTest(idOfPersonnelClassGroup(classId), "Groupe du personnel de " + classId), classId, Profile.Personnel);
        withClassGroup(new GroupTest(idOfGuestClassGroup(classId), "Groupe des invités de " + classId), classId, Profile.Guest);
        withClassGroup(new GroupTest(idOfStudentClassGroup(classId), "Groupe des étudiants de " + classId), classId, Profile.Student);
        return this;
    }

    /**
     * A student of a class :
     * <ul>
     *     <li>is a node :User...</li>
     *     <li>which is IN the student :ProfileGroup of the structure...</li>
     *     <li>which is IN the student :ProfileGroup of the class</li>
     * </ul>
     * @param studentId Id of the student to add to the class
     * @param classId Id of the class
     * @return this helper
     */
    public DataHelper studentInClass(final String studentId, final String classId) {
        sb.add("MATCH (u:User{id: {studentId}}) WITH u " +
                        "MATCH (cpg:ProfileGroup{filter: 'Student'})-[:DEPENDS]->(c:Class{id: {classId}})-[:BELONGS]->(s:Structure)<-[:DEPENDS]-(spg:ProfileGroup{filter: 'Student'}) " +
                        "MERGE (cpg)<-[:IN]-(u)-[:IN]->(spg)",
                new JsonObject()
                        .put("classId", classId)
                        .put("studentId", studentId));
        return this;
    }

    /**
     * A teacher of a class :
     * <ul>
     *     <li>is linked to the teacher profile group of the structure</li>
     *     <li>is linked to the teacher profile group of the class</li>
     *     <li>has an administrative attachment to the structure</li>
     * </ul>
     * @param teacherId Id of the teacher to add to the class
     * @param classId Id of the class
     * @return this helper
     */
    public DataHelper teacherInClass(final String teacherId, final String classId) {
        sb.add("MATCH (u:User{id: {teacherId}}) WITH u " +
                     "MATCH (cpg:ProfileGroup{filter: {pgFilter}})-[:DEPENDS]->(c:Class{id: {classId}})-[:BELONGS]->(s:Structure)<-[:DEPENDS]-(spg:ProfileGroup{filter: {pgFilter}}) " +
                     "MERGE (cpg)<-[:IN]-(u)-[:IN]->(spg) " +
                     "MERGE (s)<-[:ADMINISTRATIVE_ATTACHMENT]-(u)",
                new JsonObject()
                        .put("classId", classId)
                        .put("teacherId", teacherId)
                        .put("pgFilter", Profile.Teacher.name));
        return this;
    }

    /**
     * A parent :
     * <ul>
     *     <li>is in the ProfileGroup of the relatives of the class of the child</li>
     *     <li>has an incoming RELATED link to the child</li>
     * </ul>
     * @param parentId id of the parent
     * @param childId id of the child
     * @return this helper
     */
    public DataHelper parentOf(final String parentId, final String childId) {
        sb.add("MATCH (parent:User{id: {parentId}}), " +
                     "(child:User{id: {childId}})-[:IN]->(:ProfileGroup{filter: {studentGroupFilter}})-[:DEPENDS]->(c:Class)<-[:DEPENDS]-(relativesGroup:ProfileGroup{filter: {relativeGroupFilter}}) " +
                     "WITH parent, child, c, relativesGroup " +
                     "MATCH (c)-[:BELONGS]->(:Structure)<-[:DEPENDS]-(spg:ProfileGroup{filter: {relativeGroupFilter}}) " +
                     "MERGE (parent)<-[:RELATED]-(child) " +
                     "MERGE (relativesGroup)<-[:IN]-(parent)-[:IN]->(spg)",
                new JsonObject()
                        .put("parentId", parentId)
                        .put("childId", childId)
                        .put("relativeGroupFilter", Profile.Relative.name)
                        .put("studentGroupFilter", Profile.Student.name));
        return this;
    }

    /**
     * An ADML :
     * <ul>is linked to a function ADML whose scope contains the list of the structures of which he/she is ADML</ul>
     * <ul>is in the FunctionGroup ADMIN_LOCAL of the structures of which he/she is ADML</ul>
     * @param admlId Id of the user to be an adml
     * @param structureId Id of the structure who is administered by this user
     * @return This helper
     */
    public DataHelper adml(final String admlId, final String... structureId) {
        sb.add("MATCH (adml:User{id: {admlId}})," +
                        "    (s:Structure)<-[:DEPENDS]-(fg:FunctionGroup{filter: {adminLocalFilter}}) " +
                        "         WHERE s.id in {structureId}" +
                      "WITH adml, s, fg " +
                      "MATCH (s)<-[:DEPENDS]-(spg:ProfileGroup{filter: {personnelFilter}})-[:HAS_PROFILE]->(:Profile)<-[:COMPOSE]-(function:Function{name: {adminLocalFilter}})" +
                      "MERGE (function)<-[:HAS_FUNCTION{scope: {scope}}]-(adml)-[:IN{source: 'MANUAL'}]->(fg)",
                new JsonObject()
                        .put("admlId", admlId)
                        .put("structureId", toJsonArray(Arrays.asList(structureId)))
                        .put("adminLocalFilter", Function.AdminLocal.name())
                        .put("personnelFilter", Profile.Personnel.name)
                        .put("scope", toJsonArray(Arrays.asList(structureId))));
        return this;
    }


    private DataHelper withClassGroup(final GroupTest group, final String classId, final Profile profile) {
        sb.add("MATCH (c:Class{id: {classId}})-[:BELONGS]->(s:Structure)<-[:DEPENDS]-(spg:ProfileGroup)-[:HAS_PROFILE]->(:Profile{id: {profileId}}) " +
                      "MERGE (c)<-[:DEPENDS]-(:Group:ProfileGroup{id: {groupId}, name: {groupName}, filter:{spgFilter}})-[:DEPENDS]->(spg)",
                new JsonObject()
                        .put("classId", classId)
                        .put("groupId", group.getId())
                        .put("groupName", group.getName())
                        .put("profileId", profile.id)
                        .put("spgFilter", profile.name));
        return this;
    }

    /**
     * Execute stored queries then empties the list of executed actions if the transaction was successful so this object
     * can then be reused.
     * @return when the transaction ends
     */
    public Future<Void> execute() {
        final Promise<Void> promise = Promise.promise();
        neo4j.executeTransaction(sb.build(), null, true, e -> {
            if ("ok".equals(e.body().getString("status"))) {
                clear();
                promise.complete();
            } else {
                promise.fail(e.body().encodePrettily());
            }
        });
        return promise.future();
    }

    /**
     * Empty the queries to execute.
     * @return this helper
     */
    public DataHelper clear() {
        sb = new StatementsBuilder();
        return this;
    }

    private static String idOfParentClassGroup(final String classId) {
        return classId + "-parent";
    }

    private static String idOfTeacherClassGroup(final String classId) {
        return classId + "-teacher";
    }

    private static String idOfPersonnelClassGroup(final String classId) {
        return classId + "-personnel";
    }
    private static String idOfGuestClassGroup(final String classId) {
        return classId + "-guest";
    }
    private static String idOfStudentClassGroup(final String classId) {
        return classId + "-student";
    }

}
