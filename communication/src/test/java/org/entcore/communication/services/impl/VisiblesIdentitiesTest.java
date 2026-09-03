package org.entcore.communication.services.impl;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.entcore.common.neo4j.Neo4j;
import org.entcore.common.notification.TimelineHelper;
import org.entcore.common.user.dto.VisibleIdentityRequest;
import org.entcore.test.TestHelper;
import org.entcore.test.preparation.DataHelper;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.testcontainers.containers.Neo4jContainer;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * The user vi.user is in vi.own (BOTH), which communicates with vi.profile. vi.profile has a member vi.member
 * and an empty manual group vi.empty depending on it. vi.own also has an empty manual group vi.own-empty
 * depending on it. vi.outsider and vi.outsider-group (BOTH, vi.outsider being its member) are not visible at all.
 */
@RunWith(VertxUnitRunner.class)
public class VisiblesIdentitiesTest {
    private static final TestHelper test = TestHelper.helper();
    @ClassRule
    public static Neo4jContainer<?> neo4jContainer = test.database().createNeo4jContainer();
    private static DefaultCommunicationService service;

    @BeforeClass
    public static void setUp(TestContext context) {
        final Vertx vertx = test.vertx();
        DataHelper.init(context, neo4jContainer);
        service = new DefaultCommunicationService(vertx, new TimelineHelper(vertx, vertx.eventBus(), new JsonObject()), new JsonObject());
        final String fixture =
                "CREATE (u:User {id: 'vi.user', displayName: 'Vi User'}), " +
                "(m:User {id: 'vi.member', displayName: 'Vi Member', displayNameSearchField: 'vimember'}), " +
                "(o:User {id: 'vi.outsider', displayName: 'Vi Outsider', displayNameSearchField: 'vioutsider'}), " +
                "(og:Group:ManualGroup {id: 'vi.outsider-group', name: 'outsider group', users: 'BOTH', nbUsers: 1}), " +
                "(own:Group:ProfileGroup {id: 'vi.own', name: 'own', users: 'BOTH', nbUsers: 1, communiqueWith: ['vi.profile']}), " +
                "(p:Group:ProfileGroup {id: 'vi.profile', name: 'profile', users: 'BOTH', nbUsers: 1}), " +
                "(e:Group:ManualGroup {id: 'vi.empty', name: 'empty', nbUsers: 0}), " +
                "(oe:Group:ManualGroup {id: 'vi.own-empty', name: 'own empty', nbUsers: 0}), " +
                "(u)-[:IN]->(own), (m)-[:IN]->(p), (o)-[:IN]->(og), (e)-[:DEPENDS]->(p), (oe)-[:DEPENDS]->(own)";
        final Promise<Void> promise = Promise.promise();
        Neo4j.getInstance().execute(fixture, new JsonObject(), e -> {
            if ("ok".equals(e.body().getString("status"))) {
                promise.complete();
            } else {
                promise.fail(e.body().encode());
            }
        });
        promise.future().onComplete(context.asyncAssertSuccess());
    }

    @Test
    public void testBothReturnsUsersAndNonEmptyGroups(TestContext context) {
        visibleIds(new VisibleIdentityRequest().setUserId("vi.user"))
                .onComplete(context.asyncAssertSuccess(ids -> context.assertEquals(
                        new HashSet<>(Arrays.asList("vi.member", "vi.own", "vi.profile")), ids)));
    }

    @Test
    public void testGroupsReturnsOnlyNonEmptyGroups(TestContext context) {
        visibleIds(new VisibleIdentityRequest().setUserId("vi.user")
                .setVisibleIdFilter(VisibleIdentityRequest.VisibleIdFilter.GROUPS))
                .onComplete(context.asyncAssertSuccess(ids -> context.assertEquals(
                        new HashSet<>(Arrays.asList("vi.own", "vi.profile")), ids)));
    }

    @Test
    public void testGroupsWithEmptyGroups(TestContext context) {
        visibleIds(new VisibleIdentityRequest().setUserId("vi.user")
                .setVisibleIdFilter(VisibleIdentityRequest.VisibleIdFilter.GROUPS)
                .setIncludeEmptyGroups(true))
                .onComplete(context.asyncAssertSuccess(ids -> context.assertEquals(
                        new HashSet<>(Arrays.asList("vi.own", "vi.profile", "vi.empty", "vi.own-empty")), ids)));
    }

    @Test
    public void testGroupsRestrictedToExpectedIds(TestContext context) {
        visibleIds(new VisibleIdentityRequest().setUserId("vi.user")
                .setVisibleIdFilter(VisibleIdentityRequest.VisibleIdFilter.GROUPS)
                .setIncludeEmptyGroups(true)
                .setExpectedVisiblesIds(Arrays.asList("vi.empty", "vi.member")))
                .onComplete(context.asyncAssertSuccess(ids -> context.assertEquals(
                        new HashSet<>(Arrays.asList("vi.empty")), ids)));
    }

    @Test
    public void testBothRestrictedToExpectedIds(TestContext context) {
        visibleIds(new VisibleIdentityRequest().setUserId("vi.user")
                .setExpectedVisiblesIds(Arrays.asList("vi.member", "vi.outsider", "vi.profile", "vi.outsider-group")))
                .onComplete(context.asyncAssertSuccess(ids -> context.assertEquals(
                        new HashSet<>(Arrays.asList("vi.member", "vi.profile")), ids)));
    }

    @Test
    public void testBothRestrictedToExpectedIdsKeepsSearch(TestContext context) {
        visibleIds(new VisibleIdentityRequest().setUserId("vi.user")
                .setSearch("outsider")
                .setExpectedVisiblesIds(Arrays.asList("vi.member", "vi.outsider")))
                .onComplete(context.asyncAssertSuccess(ids -> context.assertEquals(new HashSet<>(), ids)));
    }

    @Test
    public void testIncludedIdentitiesIgnoreSearchAndKeepGroups(TestContext context) {
        visibles(new VisibleIdentityRequest().setUserId("vi.user")
                .setSearch("member")
                .setIncludedVisibleIds(Arrays.asList("vi.outsider", "vi.outsider-group")))
                .onComplete(context.asyncAssertSuccess(visibles -> {
                    context.assertEquals(new HashSet<>(Arrays.asList(
                            "vi.member", "vi.own", "vi.profile", "vi.outsider", "vi.outsider-group")), ids(visibles));
                    context.assertEquals(Boolean.TRUE, isUser(visibles, "vi.outsider"));
                    context.assertEquals(Boolean.FALSE, isUser(visibles, "vi.outsider-group"));
                }));
    }

    @Test
    public void testIncludedIdentitiesWithGroupsKeepOnlyGroups(TestContext context) {
        visibleIds(new VisibleIdentityRequest().setUserId("vi.user")
                .setVisibleIdFilter(VisibleIdentityRequest.VisibleIdFilter.GROUPS)
                .setIncludedVisibleIds(Arrays.asList("vi.outsider", "vi.outsider-group")))
                .onComplete(context.asyncAssertSuccess(ids -> context.assertEquals(
                        new HashSet<>(Arrays.asList("vi.own", "vi.profile", "vi.outsider-group")), ids)));
    }

    @Test
    public void testIncludedIdentitiesWithUsersOfGroupsKeepOnlyUsers(TestContext context) {
        visibleIds(new VisibleIdentityRequest().setUserId("vi.user")
                .setVisibleIdFilter(VisibleIdentityRequest.VisibleIdFilter.USERS_OF_GROUPS)
                .setSearch("member")
                .setIncludedVisibleIds(Arrays.asList("vi.outsider", "vi.outsider-group")))
                .onComplete(context.asyncAssertSuccess(ids -> context.assertEquals(
                        new HashSet<>(Arrays.asList("vi.member", "vi.outsider")), ids)));
    }

    @Test
    public void testPublicDetailsGiveGroupTypeOnGroupsOnly(TestContext context) {
        visibles(new VisibleIdentityRequest().setUserId("vi.user")
                .setPublicDetails(true)
                .setIncludedVisibleIds(Arrays.asList("vi.outsider", "vi.outsider-group")))
                .onComplete(context.asyncAssertSuccess(visibles -> {
                    final JsonObject profileGroup = byId(visibles, "vi.profile");
                    context.assertEquals("ProfileGroup", profileGroup.getString("groupType"));
                    context.assertEquals(1, profileGroup.getInteger("nbUsers"));
                    context.assertEquals("ManualGroup", byId(visibles, "vi.outsider-group").getString("groupType"));
                    context.assertNull(byId(visibles, "vi.member").getString("groupType"));
                    context.assertNull(byId(visibles, "vi.outsider").getString("groupType"));
                }));
    }

    private static JsonObject byId(JsonArray visibles, String id) {
        return visibles.stream().map(JsonObject.class::cast)
                .filter(o -> id.equals(o.getString("id")))
                .findFirst().orElseThrow(() -> new AssertionError(id + " missing from " + visibles.encode()));
    }

    private static Set<String> ids(JsonArray visibles) {
        return visibles.stream().map(o -> ((JsonObject) o).getString("id")).collect(Collectors.toSet());
    }

    private static Boolean isUser(JsonArray visibles, String id) {
        return visibles.stream().map(JsonObject.class::cast)
                .filter(o -> id.equals(o.getString("id")))
                .map(o -> o.getBoolean("isUser"))
                .findFirst().orElse(null);
    }

    private Future<Set<String>> visibleIds(VisibleIdentityRequest request) {
        return visibles(request).map(VisiblesIdentitiesTest::ids);
    }

    private Future<JsonArray> visibles(VisibleIdentityRequest request) {
        final Promise<JsonArray> promise = Promise.promise();
        service.visiblesIdentities(request, r -> {
            if (r.isRight()) {
                promise.complete(r.right().getValue());
            } else {
                promise.fail(r.left().getValue());
            }
        });
        return promise.future();
    }
}
