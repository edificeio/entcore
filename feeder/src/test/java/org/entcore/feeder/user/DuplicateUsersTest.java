package org.entcore.feeder.user;

import io.vertx.core.Context;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.entcore.common.events.EventStoreFactory;
import org.entcore.common.neo4j.Neo4j;
import org.entcore.feeder.Feeder;
import org.entcore.feeder.dictionary.structures.DuplicateUsers;
import org.entcore.feeder.dictionary.structures.RelationshipToKeepForDuplicatedUser;
import org.entcore.feeder.exceptions.TransactionException;
import org.entcore.feeder.test.integration.java.FeederTest;
import org.entcore.feeder.timetable.AbstractTimetableImporter;
import org.entcore.feeder.utils.TransactionHelper;
import org.entcore.feeder.utils.TransactionManager;
import org.entcore.feeder.utils.Validator;
import org.entcore.test.TestHelper;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.testcontainers.containers.Neo4jContainer;

import java.util.List;

@RunWith(VertxUnitRunner.class)
public class DuplicateUsersTest {
    private static final TestHelper test = TestHelper.helper();
    @ClassRule
    public static Neo4jContainer<?> neo4jContainer = test.database().createNeo4jContainer();

    private static DuplicateUsers duplicateUsers;

    @BeforeClass
    public static void setUp(TestContext context) throws Exception {
        EventStoreFactory.getFactory().setVertx(test.vertx());
        duplicateUsers = new DuplicateUsers(false, false, test.vertx().eventBus());
        test.database().initNeo4j(context, neo4jContainer);
        final String base = neo4jContainer.getHttpUrl() + "/db/data/";
        final JsonObject neo4jConfig = new JsonObject()
                .put("server-uri", base).put("poolSize", 1);
        final Neo4j neo4j = Neo4j.getInstance();
        neo4j.init(test.vertx(), neo4jConfig
                .put("server-uri", base)
                .put("ignore-empty-statements-error", false));
        Validator.initLogin(neo4j, test.vertx());
        TransactionManager.getInstance().setNeo4j(neo4j);
        prepareData().onComplete(context.asyncAssertSuccess());
    }

    @Test
    public void testFecthDataWhenWeDontWantToKeepRss(final TestContext context) {
        final Async async = context.async();
        duplicateUsers.fetchRelationshipsToKeep(false)
        .onSuccess(rss -> {
            context.assertNotNull(rss, "Should not have fetched relationships but the returned object should not be null");
            context.assertTrue(rss.getUserRelationship("user1").isEmpty(), "Should not have fetched relationships but the returned object should not be null");
            context.assertTrue(rss.getUserRelationship("user2").isEmpty(), "Should not have fetched relationships but the returned object should not be null");
            async.complete();
        })
        .onFailure(e -> context.fail(e));
    }

    @Test
    public void testFecthDataWhenWeWantToKeepRss(final TestContext context) {
        final Async async = context.async();
        duplicateUsers.fetchRelationshipsToKeep(true, "user1", "user2")
        .onSuccess(rss -> {
            context.assertNotNull(rss, "Should have fetched relationships");
            final List<RelationshipToKeepForDuplicatedUser> user1Rss = rss.getUserRelationship("user1");
            context.assertEquals(4, user1Rss.size(), "Should have fetched relationships of user 1");
            context.assertTrue(rss.isUserHasRs("user1", "IN", "struct1", true));
            context.assertTrue(rss.isUserHasRs("user1", "IN", "struct2", true));
            context.assertTrue(rss.isUserHasRs("user1", "RELATED", "user1Child1", false));
            context.assertTrue(rss.isUserHasRs("user1", "RELATED", "user1Child2", false));
            final List<RelationshipToKeepForDuplicatedUser> user2Rss = rss.getUserRelationship("user2");
            context.assertEquals(2, user2Rss.size(), "Should have fetched relationships of user 2");
            context.assertTrue(rss.isUserHasRs("user2", "IN", "struct2", true));
            context.assertTrue(rss.isUserHasRs("user2", "RELATED", "user2Child1", false));
            async.complete();
        })
        .onFailure(e -> context.fail(e));
    }

    public static Future<Void> prepareData() {
        final Promise<Void> promise = Promise.promise();
        TransactionHelper txl = null;
        try {
            txl = TransactionManager.getTransaction();
            // User 1 :
            // - is in structure 1 and structure 2
            // - has 2 kids user1Child1 and user1Child2
            // - can communicate with structure 1 (incoming) and structure 2 (outgoing)
            // User 2 in only in structure 2 and has 1 kid user2Child1
            // - can communicate with structure 2 (incoming)
            txl.add("create (u1:User{id: 'user1'})-[:IN{source:'CSV'}]->(s1:Group:ProfileGroup:Visible{id: 'struct1'})", new JsonObject());
            txl.add("create (u2:User{id: 'user2'})-[:IN{source:'AAF1D'}]->(s1:Group:ProfileGroup:Visible{id: 'struct2'})", new JsonObject());
            txl.add("MATCH (u1:User{id: 'user1'}), (s2:Group{id: 'struct2'}) CREATE (u1)-[:IN{source:'CSV'}]->(s2)", new JsonObject());
            txl.add("MATCH (u1:User{id: 'user1'}) CREATE (u1)<-[:RELATED]-(:User{id: 'user1Child1'})", new JsonObject());
            txl.add("MATCH (u1:User{id: 'user1'}) CREATE (u1)<-[:RELATED]-(:User{id: 'user1Child2'})", new JsonObject());
            txl.add("MATCH (u2:User{id: 'user2'}) CREATE (u2)<-[:RELATED]-(:User{id: 'user2Child1'})", new JsonObject());
            txl.add("MATCH (u1:User{id: 'user1'}) MATCH (s1:Group{id: 'struct1'}) CREATE (s1)-[:COMMUNIQUE]->(u1)", new JsonObject());
            txl.add("MATCH (u1:User{id: 'user1'}) MATCH (s2:Group{id: 'struct2'}) CREATE (s2)<-[:COMMUNIQUE]-(u1)", new JsonObject());
            txl.add("MATCH (u2:User{id: 'user2'}) MATCH (s2:Group{id: 'struct2'}) CREATE (s2)-[:COMMUNIQUE]->(u1)", new JsonObject());
            txl.commit(new Handler<Message<JsonObject>>() {
                @Override
                public void handle(Message<JsonObject> event) {
                    if ("ok".equals(event.body().getString("status"))) {
                       promise.complete();
                    } else {
                        promise.fail(event.body().encodePrettily());
                    }
                }
            });
        } catch (TransactionException e) {
            promise.fail(e);
        }
        return promise.future();
    }

}
