package org.entcore.directory.services.impl;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.entcore.common.events.EventStoreFactory;
import org.entcore.test.TestHelper;
import org.entcore.test.preparation.DataHelper;
import org.entcore.test.preparation.Profile;
import org.entcore.test.preparation.StructureTest;
import org.entcore.test.preparation.StructureTestBuilder;
import org.entcore.test.preparation.UserTest;
import org.entcore.test.preparation.UserTestBuilder;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.testcontainers.containers.Neo4jContainer;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Tests {@code getUsersStructuresWithPreferred} against a real Neo4j, covering what the parsing unit tests
 * cannot: the batched Cypher query itself, and the end-to-end degradation when a preference is unusable.
 *
 * @see DefaultUserServicePreferredStructureTest for the parsing of the widgets preference blob
 */
@RunWith(VertxUnitRunner.class)
public class DefaultUserServiceStructuresWithPreferredTest {

    private static final TestHelper test = TestHelper.helper();
    @ClassRule
    public static Neo4jContainer<?> neo4jContainer = test.database().createNeo4jContainer();

    private static DefaultUserService defaultUserService;
    private static DataHelper dataHelper;

    private static final StructureTest structureA = StructureTestBuilder.aStructureTest()
            .withId("structure-a").withName("Collège Jean Moulin").withUai("0000001A").build();
    private static final StructureTest structureB = StructureTestBuilder.aStructureTest()
            .withId("structure-b").withName("Lycée Ampère").withUai("0000002B").build();
    private static final StructureTest structureUnrelated = StructureTestBuilder.aStructureTest()
            .withId("structure-unrelated").withName("Académie de Lyon").withUai("0000003C").build();

    /** Attached to two structures, prefers structure B. */
    private static final UserTest multiStructureWithPreference = UserTestBuilder.anUserTest()
            .id("user-multi-with-pref").login("multi.pref")
            .firstName("Marie").lastName("Dupont").displayName("Dupont Marie")
            .profile(Profile.Teacher).build();
    /** Attached to two structures, no preference at all. */
    private static final UserTest multiStructureNoPreference = UserTestBuilder.anUserTest()
            .id("user-multi-no-pref").login("multi.nopref")
            .firstName("Thomas").lastName("Petit").displayName("Petit Thomas")
            .profile(Profile.Teacher).build();
    /** Has a widgets preference, but it does not carry a school widget. */
    private static final UserTest otherWidgetsOnly = UserTestBuilder.anUserTest()
            .id("user-other-widgets").login("other.widgets")
            .firstName("Sophie").lastName("Bernard").displayName("Bernard Sophie")
            .profile(Profile.Personnel).build();
    /** Widgets preference stored as unparseable JSON. */
    private static final UserTest malformedPreference = UserTestBuilder.anUserTest()
            .id("user-malformed-pref").login("malformed.pref")
            .firstName("Julien").lastName("Martin").displayName("Martin Julien")
            .profile(Profile.Relative).build();
    /** Prefers a structure they are not attached to (stale preference). */
    private static final UserTest stalePreference = UserTestBuilder.anUserTest()
            .id("user-stale-pref").login("stale.pref")
            .firstName("Claire").lastName("Roux").displayName("Roux Claire")
            .profile(Profile.Teacher).build();
    /** Exists, but is attached to no structure at all. */
    private static final UserTest noStructure = UserTestBuilder.anUserTest()
            .id("user-no-structure").login("no.structure")
            .firstName("Paul").lastName("Durand").displayName("Durand Paul")
            .build();

    @BeforeClass
    public static void setUp(TestContext context) {
        final Vertx vertx = test.vertx();
        EventStoreFactory.getFactory().setVertx(vertx);
        defaultUserService = new DefaultUserService(null, vertx.eventBus(), new JsonObject()
                .put("default-avatar", "avatar.png")
                .put("default-theme", "theme"));
        dataHelper = DataHelper.init(context, neo4jContainer);
        prepareData().onComplete(context.asyncAssertSuccess());
    }

    private static Future<Void> prepareData() {
        return dataHelper.start()
                .withStructure(structureA)
                .withStructure(structureB)
                .withStructure(structureUnrelated)
                .withUser(multiStructureWithPreference, structureA.getId())
                .withUser(multiStructureWithPreference, structureB.getId())
                .withPreference(multiStructureWithPreference, "widgets",
                        new JsonObject().put("school-widget",
                                new JsonObject().put("schoolId", structureB.getId())).encode())
                .withUser(multiStructureNoPreference, structureA.getId())
                .withUser(multiStructureNoPreference, structureB.getId())
                .withUser(otherWidgetsOnly, structureA.getId())
                .withPreference(otherWidgetsOnly, "widgets",
                        new JsonObject().put("birthday", new JsonObject().put("enabled", true)).encode())
                .withUser(malformedPreference, structureA.getId())
                .withPreference(malformedPreference, "widgets", "{\"school-widget\":{\"schoolId\":")
                .withUser(stalePreference, structureA.getId())
                .withPreference(stalePreference, "widgets",
                        new JsonObject().put("school-widget",
                                new JsonObject().put("schoolId", structureUnrelated.getId())).encode())
                .withUser(noStructure)
                .execute();
    }

    private Future<Map<String, JsonObject>> resolve(final String... userIds) {
        final Promise<Map<String, JsonObject>> promise = Promise.promise();
        defaultUserService.getUsersStructuresWithPreferred(new JsonArray(Arrays.asList(userIds)), res -> {
            if (res.isLeft()) {
                promise.fail(res.left().getValue());
                return;
            }
            promise.complete(res.right().getValue().stream()
                    .map(JsonObject.class::cast)
                    .collect(Collectors.toMap(u -> u.getString("id"), u -> u)));
        });
        return promise.future();
    }

    private static List<String> structureIdsOf(final JsonObject user) {
        return user.getJsonArray("structures").stream()
                .map(JsonObject.class::cast)
                .map(s -> s.getString("id"))
                .sorted()
                .collect(Collectors.toList());
    }

    @Test
    public void shouldReturnStructuresAndPreferredStructureId(final TestContext context) {
        resolve(multiStructureWithPreference.getId()).onComplete(context.asyncAssertSuccess(byId -> {
            final JsonObject user = byId.get(multiStructureWithPreference.getId());
            context.assertNotNull(user);
            context.assertEquals(Arrays.asList(structureA.getId(), structureB.getId()), structureIdsOf(user));
            context.assertEquals(structureB.getId(), user.getString("preferredStructureId"));
        }));
    }

    @Test
    public void shouldReturnStructureNamesSoCallersNeedNoSecondLookup(final TestContext context) {
        resolve(multiStructureWithPreference.getId()).onComplete(context.asyncAssertSuccess(byId -> {
            final List<String> names = byId.get(multiStructureWithPreference.getId())
                    .getJsonArray("structures").stream()
                    .map(JsonObject.class::cast)
                    .map(s -> s.getString("name"))
                    .sorted()
                    .collect(Collectors.toList());
            context.assertEquals(Arrays.asList(structureA.getName(), structureB.getName()), names);
        }));
    }

    @Test
    public void withoutAnyPreference_shouldOmitPreferredStructureId(final TestContext context) {
        resolve(multiStructureNoPreference.getId()).onComplete(context.asyncAssertSuccess(byId -> {
            final JsonObject user = byId.get(multiStructureNoPreference.getId());
            context.assertNotNull(user);
            context.assertEquals(2, user.getJsonArray("structures").size());
            context.assertFalse(user.containsKey("preferredStructureId"));
        }));
    }

    @Test
    public void withWidgetsButNoSchoolWidget_shouldOmitPreferredStructureId(final TestContext context) {
        resolve(otherWidgetsOnly.getId()).onComplete(context.asyncAssertSuccess(byId -> {
            final JsonObject user = byId.get(otherWidgetsOnly.getId());
            context.assertNotNull(user);
            context.assertFalse(user.containsKey("preferredStructureId"));
        }));
    }

    @Test
    public void withMalformedPreference_shouldStillReturnTheUserWithoutPreferred(final TestContext context) {
        // The whole point of the defensive parsing: one unusable blob must not fail the batch.
        resolve(malformedPreference.getId()).onComplete(context.asyncAssertSuccess(byId -> {
            final JsonObject user = byId.get(malformedPreference.getId());
            context.assertNotNull(user);
            context.assertEquals(Arrays.asList(structureA.getId()), structureIdsOf(user));
            context.assertFalse(user.containsKey("preferredStructureId"));
        }));
    }

    @Test
    public void withStalePreference_shouldReturnItEvenIfNotAmongStructures(final TestContext context) {
        // Documented in the contract: preferredStructureId is not guaranteed to be one of `structures`.
        resolve(stalePreference.getId()).onComplete(context.asyncAssertSuccess(byId -> {
            final JsonObject user = byId.get(stalePreference.getId());
            context.assertNotNull(user);
            context.assertEquals(Arrays.asList(structureA.getId()), structureIdsOf(user));
            context.assertEquals(structureUnrelated.getId(), user.getString("preferredStructureId"));
        }));
    }

    @Test
    public void withoutAnyStructure_shouldBeAbsentFromTheResult(final TestContext context) {
        // A user attached to no structure has nothing to display, so the query does not return them at all.
        // Callers must not assume every requested id comes back.
        resolve(noStructure.getId()).onComplete(context.asyncAssertSuccess(byId ->
                context.assertEquals(0, byId.size())));
    }

    @Test
    public void shouldResolveTheWholeBatchInASingleCall(final TestContext context) {
        resolve(multiStructureWithPreference.getId(), multiStructureNoPreference.getId(),
                malformedPreference.getId(), stalePreference.getId(), noStructure.getId())
                .onComplete(context.asyncAssertSuccess(byId -> {
                    // noStructure is legitimately absent: 5 requested, 4 resolved.
                    context.assertEquals(4, byId.size());
                    context.assertEquals(structureB.getId(),
                            byId.get(multiStructureWithPreference.getId()).getString("preferredStructureId"));
                    context.assertFalse(byId.get(multiStructureNoPreference.getId())
                            .containsKey("preferredStructureId"));
                    context.assertFalse(byId.get(malformedPreference.getId())
                            .containsKey("preferredStructureId"));
                    context.assertEquals(structureUnrelated.getId(),
                            byId.get(stalePreference.getId()).getString("preferredStructureId"));
                }));
    }

    @Test
    public void unknownUsers_shouldBeAbsentFromTheResultRatherThanFail(final TestContext context) {
        resolve("does-not-exist", multiStructureWithPreference.getId())
                .onComplete(context.asyncAssertSuccess(byId -> {
                    context.assertEquals(1, byId.size());
                    context.assertNull(byId.get("does-not-exist"));
                    context.assertNotNull(byId.get(multiStructureWithPreference.getId()));
                }));
    }

    @Test
    public void shouldNeverLeakTheRawPreferenceBlob(final TestContext context) {
        // The widgets preference holds unrelated user preferences; only the resolved id may be exposed.
        resolve(multiStructureWithPreference.getId(), otherWidgetsOnly.getId())
                .onComplete(context.asyncAssertSuccess(byId -> byId.values().forEach(user ->
                        context.assertFalse(user.containsKey("widgets"), "raw widgets blob leaked: " + user))));
    }

    @Test
    public void emptyInput_shouldReturnAnEmptyResultWithoutQuerying(final TestContext context) {
        resolve().onComplete(context.asyncAssertSuccess(byId -> context.assertEquals(0, byId.size())));
    }
}