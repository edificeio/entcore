package org.entcore.conversation;

import java.util.Arrays;
import java.util.Collections;

import org.entcore.conversation.service.impl.Neo4jConversationService;
import org.entcore.test.TestHelper;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.testcontainers.containers.Neo4jContainer;

import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;

/**
 * Tests the resolution of the absent users identities : what a session would carry and the absence reply
 * notification reads (display name, main profile), plus the language its subject is translated in.
 */
@RunWith(VertxUnitRunner.class)
public class UsersIdentitiesTest {

    private static final TestHelper test = TestHelper.helper();

    @ClassRule
    public static Neo4jContainer<?> neo4jContainer = test.database().createNeo4jContainer();

    private static Neo4jConversationService service;

    @BeforeClass
    public static void setUp(TestContext context) {
        test.database().initNeo4j(context, neo4jContainer);
        service = new Neo4jConversationService();
        createUser("with-pref", "Marie Curie", "Teacher", "{\"default-domain\":\"en\"}")
            .compose(v -> createUser("without-pref", "Pierre Curie", "Personnel", null))
            .compose(v -> createUser("bad-pref", "Irène Curie", "Student", "not json"))
            .onComplete(context.asyncAssertSuccess());
    }

    private static Future<Void> createUser(final String id, final String displayName, final String profile,
            final String language) {
        final JsonObject params = new JsonObject().put("id", id).put("displayName", displayName)
                .put("profile", profile).put("language", language);
        final String query = language == null
                ? "CREATE (:User {id: {id}, displayName: {displayName}, profiles: [{profile}]})"
                : "CREATE (:User {id: {id}, displayName: {displayName}, profiles: [{profile}]})"
                    + "-[:PREFERS]->(:UserAppConf {language: {language}})";
        return test.database().executeNeo4j(query, params).mapEmpty();
    }

    @Test
    public void userWithPreferenceShouldHaveFullIdentity(TestContext context) {
        service.findUsersIdentities(Collections.singletonList("with-pref"), 10)
            .onComplete(context.asyncAssertSuccess(identities -> context.assertEquals(
                new JsonObject().put("username", "Marie Curie").put("type", "Teacher").put("language", "en"),
                identities.getJsonObject("with-pref"))));
    }

    /** No preference must not cost the notification its name and profile : only the language is missing. */
    @Test
    public void userWithoutPreferenceShouldHaveNameAndProfileOnly(TestContext context) {
        service.findUsersIdentities(Arrays.asList("without-pref", "bad-pref"), 10)
            .onComplete(context.asyncAssertSuccess(identities -> {
                context.assertEquals(new JsonObject().put("username", "Pierre Curie").put("type", "Personnel"),
                        identities.getJsonObject("without-pref"));
                context.assertEquals(new JsonObject().put("username", "Irène Curie").put("type", "Student"),
                        identities.getJsonObject("bad-pref"));
            }));
    }

    @Test
    public void emptyListShouldResolveToEmptyMapping(TestContext context) {
        service.findUsersIdentities(Collections.emptyList(), 10)
            .onComplete(context.asyncAssertSuccess(identities -> context.assertTrue(identities.isEmpty())));
    }

    @Test
    public void everyBatchShouldBeMerged(TestContext context) {
        service.findUsersIdentities(Arrays.asList("with-pref", "without-pref", "unknown"), 1)
            .onComplete(context.asyncAssertSuccess(identities -> {
                context.assertEquals(2, identities.size());
                context.assertEquals("Marie Curie", identities.getJsonObject("with-pref").getString("username"));
                context.assertEquals("Pierre Curie", identities.getJsonObject("without-pref").getString("username"));
            }));
    }
}
