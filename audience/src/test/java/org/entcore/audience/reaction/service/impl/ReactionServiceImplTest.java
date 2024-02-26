package org.entcore.audience.reaction.service.impl;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.eventbus.MessageConsumer;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.entcore.audience.reaction.dao.impl.ReactionDaoImpl;
import org.entcore.audience.reaction.model.ReactionsSummaryForResource;
import org.entcore.audience.reaction.model.UserReaction;
import org.entcore.audience.reaction.service.ReactionService;
import org.entcore.common.sql.Sql;
import org.entcore.common.user.UserInfos;
import org.entcore.test.TestHelper;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.testcontainers.containers.PostgreSQLContainer;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@RunWith(VertxUnitRunner.class)
public class ReactionServiceImplTest {

  private static ReactionService reactionService;

  private static final TestHelper test = TestHelper.helper();

  @ClassRule
  public static PostgreSQLContainer<?> pgContainer = test.database().createPostgreSQLContainer()
      .withInitScript("initAudience.sql")
      .withReuse(true);
  private static final String schema = "audience";


  @BeforeClass
  public static void setUp(TestContext context) {
    final Async async = context.async();
    test.database().initPostgreSQL(context, pgContainer, schema).handler(e -> {
      if(e.succeeded()) {
        insertMockData().onFailure(context::fail).onSuccess(s -> async.complete());
      } else {
        context.fail(e.cause());
      }
    });
    final ReactionDaoImpl reactionDao = new ReactionDaoImpl(Sql.getInstance());

    // Mock directory consumer
    MessageConsumer<JsonObject> consumer = test.vertx().eventBus().consumer("entcore.directory");
    consumer.handler(message -> {
        String action = message.body().getString("action", "action.not.specified");
        if (action.equals("get-users-displayNames")) {
            JsonObject displayNamesByUserId = new JsonObject()
                    .put("user-id", "User Id")
                    .put("user-id-0", "User Id 0")
                    .put("user-id-1", "User Id 1")
                    .put("user-id-2", "User Id 2");
            message.reply(displayNamesByUserId);
        } else {
            message.fail(500, "[Directory] " + action);
        }
    });

    reactionService = new ReactionServiceImpl(test.vertx().eventBus(), reactionDao);
  }

  @Test
  public void testGetReactionsSummaryWhenNoData(final TestContext context) {
    final UserInfos userInfos = new UserInfos();
    userInfos.setUserId("user-id");
    userInfos.setFirstName("first-name");
    userInfos.setLastName("last-name");
    final Async async = context.async();
    reactionService.getReactionsSummary("module", "resource-type", Sets.newHashSet("id-1"), userInfos)
    .onFailure(context::fail)
    .onSuccess(summary -> {
      context.assertNotNull(summary, "getReactionsSummary should never return null");
      context.assertNotNull(summary.getReactionsByResource(), "getReactionsSummary should never return null");
      context.assertEquals(0, summary.getReactionsByResource().size(), "getReactionsSummary should return no data when the filter matches no data");
      async.complete();
    });
  }

  @Test
  public void testGetReactionsSummaryWhenNoResourceIdsProvided(final TestContext context) {
    final UserInfos userInfos = new UserInfos();
    userInfos.setUserId("user-id");
    userInfos.setFirstName("first-name");
    userInfos.setLastName("last-name");
    final Async async = context.async();
    reactionService.getReactionsSummary("module", "resource-type", Sets.newHashSet(), userInfos)
        .onFailure(context::fail)
        .onSuccess(summary -> {
          context.assertNotNull(summary, "getReactionsSummary should never return null");
          context.assertNotNull(summary.getReactionsByResource(), "getReactionsSummary should never return null");
          context.assertEquals(0, summary.getReactionsByResource().size(), "getReactionsSummary should return no data when the filter matches no data");
          async.complete();
        });
  }

  @Test
  public void testGetReactionsSummaryWhenResourceIdsAreNull(final TestContext context) {
    final UserInfos userInfos = new UserInfos();
    userInfos.setUserId("user-id");
    userInfos.setFirstName("first-name");
    userInfos.setFirstName("last-name");
    final Async async = context.async();
    reactionService.getReactionsSummary("module", "resource-type", null, userInfos)
        .onFailure(context::fail)
        .onSuccess(summary -> {
          context.assertNotNull(summary, "getReactionsSummary should never return null");
          context.assertNotNull(summary.getReactionsByResource(), "getReactionsSummary should never return null");
          context.assertEquals(0, summary.getReactionsByResource().size(), "getReactionsSummary should return no data when the filter matches no data");
          async.complete();
        });
  }

  @Test
  public void testGetReactionsSummaryWithOneExistingResource(final TestContext context) {
    final Async async = context.async();
    final UserInfos userInfos = new UserInfos();
    userInfos.setUserId("user-id");
    userInfos.setFirstName("first-name");
    userInfos.setLastName("last-name");
    reactionService.getReactionsSummary("mod0", "rt0", Sets.newHashSet("r-id-0"), userInfos)
        .onFailure(context::fail)
        .onSuccess(summary -> {
          context.assertNotNull(summary, "getReactionsSummary should never return null");
          context.assertNotNull(summary.getReactionsByResource(), "getReactionsSummary should never return null");
          context.assertEquals(1, summary.getReactionsByResource().size(), "getReactionsSummary should return a summary for an existing resource");
          final ReactionsSummaryForResource resSummary = summary.getReactionsByResource().get("r-id-0");
          context.assertNotNull(resSummary, "The summary of an existing resource should not be null");
          context.assertEquals(3, resSummary.getTotalReactionsCounter(), "There should be a total of 3 reactions for this resource");
          context.assertEquals(null, resSummary.getUserReaction(),"The user reaction for this resource should be NONE." );
          final Set<String> reactionTypes = resSummary.getReactionTypes();
          context.assertEquals(2, reactionTypes.size(), "There should be two types of reactions for this resource");
          context.assertTrue(reactionTypes.containsAll(Sets.newHashSet("thumb-up", "thumb-down")));
          async.complete();
        });
  }

    @Test
    public void testGetReactionsSummaryWithOneExistingResourceAndUserReaction(final TestContext context) {
        final Async async = context.async();
        final UserInfos userInfos = new UserInfos();
        userInfos.setUserId("user-id-0");
        userInfos.setFirstName("first-name");
        userInfos.setLastName("last-name");
        reactionService.getReactionsSummary("mod0", "rt0", Sets.newHashSet("r-id-0"), userInfos)
                .onFailure(context::fail)
                .onSuccess(summary -> {
                    context.assertNotNull(summary, "getReactionsSummary should never return null");
                    context.assertNotNull(summary.getReactionsByResource(), "getReactionsSummary should never return null");
                    context.assertEquals(1, summary.getReactionsByResource().size(), "getReactionsSummary should return a counter for an existing resource");
                    final ReactionsSummaryForResource resSummary = summary.getReactionsByResource().get("r-id-0");
                    context.assertNotNull(resSummary, "The summary of an existing resource should not be null");
                    context.assertEquals(3, resSummary.getTotalReactionsCounter(), "There should be a total of 3 reactions for this resource");
                    context.assertEquals("thumb-up", resSummary.getUserReaction(),"The user reaction for this resource should be thumb-up." );
                    final Set<String> reactionTypes = resSummary.getReactionTypes();
                    context.assertEquals(2, reactionTypes.size(), "There should be two types of reactions for this resource");
                    context.assertTrue(reactionTypes.containsAll(Sets.newHashSet("thumb-up", "thumb-down")));
                    async.complete();
                });
    }

  @Test
  public void testGetReactionsSummaryWithMultipleExistingResources(final TestContext context) {
    final Async async = context.async();
    final UserInfos userInfos = new UserInfos();
    userInfos.setUserId("user-id-0");
    userInfos.setFirstName("first-name");
    userInfos.setFirstName("last-name");
    reactionService.getReactionsSummary("mod0", "rt0", Sets.newHashSet("r-id-0", "r-id-1"), userInfos)
        .onFailure(context::fail)
        .onSuccess(summary -> {
          context.assertNotNull(summary, "getReactionsSummary should never return null");
          context.assertNotNull(summary.getReactionsByResource(), "getReactionsSummary should never return null");
          context.assertEquals(2, summary.getReactionsByResource().size(), "getReactionsSummary should return counters for existing resources");

          ReactionsSummaryForResource resSummary = summary.getReactionsByResource().get("r-id-0");
          context.assertEquals("thumb-up", resSummary.getUserReaction(), "The user reaction for this resource should be thumb up");
          context.assertEquals(3, resSummary.getTotalReactionsCounter(), "There should be a total of 3 reactions for this resource");
          Set<String> reactionTypes = resSummary.getReactionTypes();
          context.assertEquals(2, reactionTypes.size(), "There should be two types of reactions for this resource");
          context.assertTrue(reactionTypes.containsAll(Sets.newHashSet("thumb-up", "thumb-down")));


            resSummary = summary.getReactionsByResource().get("r-id-1");
          context.assertEquals("love", resSummary.getUserReaction(), "The user reaction for this resource should be love");
          context.assertEquals(1, resSummary.getTotalReactionsCounter(), "There should be a total of 1 reaction for this resource");
          reactionTypes = resSummary.getReactionTypes();
          context.assertEquals(1, reactionTypes.size(), "There should be one type of reactions for this resource");
          context.assertTrue(reactionTypes.containsAll(Sets.newHashSet("love")));

          async.complete();
        });
  }

  @Test
  public void testGetReactionsSummaryWithMultipleExistingResourcesAndOneAbsentResource(final TestContext context) {
    final Async async = context.async();
    final UserInfos userInfos = new UserInfos();
    userInfos.setUserId("user-id");
    userInfos.setFirstName("first-name");
    userInfos.setFirstName("last-name");
    reactionService.getReactionsSummary("mod0", "rt0", Sets.newHashSet("r-id-0", "r-id-1", "not-there"), userInfos)
        .onFailure(context::fail)
        .onSuccess(summary -> {
          context.assertNotNull(summary, "getReactionsSummary should never return null");
          context.assertNotNull(summary.getReactionsByResource(), "getReactionsSummary should never return null");
          context.assertEquals(2, summary.getReactionsByResource().size(), "getReactionsSummary should return counters for existing resources");

          ReactionsSummaryForResource resSummary = summary.getReactionsByResource().get("r-id-0");
          context.assertEquals(3, resSummary.getTotalReactionsCounter(), "There should be a total of 3 reactions for this resource");
          Set<String> reactionTypes = resSummary.getReactionTypes();
          context.assertEquals(2, reactionTypes.size(), "There should be two types of reactions for this resource");
          context.assertTrue(reactionTypes.containsAll(Sets.newHashSet("thumb-up", "thumb-down")));

          resSummary = summary.getReactionsByResource().get("r-id-1");
          reactionTypes = resSummary.getReactionTypes();
          context.assertEquals(1, resSummary.getTotalReactionsCounter(), "There should be a total of 1 reaction for this resource");
          context.assertEquals(1, reactionTypes.size(), "There should be one types of reactions for this resource");
          context.assertTrue(reactionTypes.containsAll(Sets.newHashSet("love")));

          async.complete();
        });
  }

    @Test
    public void testGetReactionDetailsWhenNoData(final TestContext context) {
        final Async async = context.async();
        reactionService.getReactionDetails("module", "resource-type", "id-1", 1, 100)
                .onFailure(context::fail)
                .onSuccess(reactionDetails -> {
                    context.assertNotNull(reactionDetails, "getReactionDetails should never return null");
                    context.assertNull(reactionDetails.getReactionCounters(), "getReactionCounters should return null when the filter matches no data");
                    context.assertTrue(reactionDetails.getUserReactions().isEmpty(), "getUserReactions should return no data when the filter matches no data");
                    async.complete();
                });
    }

  @Test
  public void testGetReactionDetails(final TestContext context) {
      final Async async = context.async();
      reactionService.getReactionDetails("mod0", "rt0", "r-id-0", 1, 100)
              .onFailure(context::fail)
              .onSuccess(reactionDetails -> {
                  context.assertNotNull(reactionDetails, "getReactionDetails should never return null");
                  context.assertNotNull(reactionDetails.getReactionCounters(), "reaction counters should never return null");
                  final Map<String, Integer> reactionCounterByType = reactionDetails.getReactionCounters().getCountByType();
                  context.assertEquals(2, reactionCounterByType.size(), "there should be 2 types of reactions for this resource");
                  context.assertEquals(1, reactionCounterByType.get("thumb-up"), "there should be 1 reaction of type thumb-up");
                  context.assertEquals(2, reactionCounterByType.get("thumb-down"), "there should be 2 reactions of type thumb-down");
                  final List<UserReaction> userReactions = reactionDetails.getUserReactions();
                  context.assertNotNull(userReactions, "list of user reactions should never return null");
                  context.assertEquals(3, userReactions.size(), "there should be 3 reactions for this resource");
                  UserReaction userReaction = userReactions.get(0);
                  context.assertEquals("user-id-1", userReaction.getUserId(), "reaction of user-id-1 should be in first position because most recent");
                  context.assertEquals("PERSRELELEVE", userReaction.getProfile(), "reaction profile should be PERSRELELEVE");
                  context.assertEquals("thumb-down", userReaction.getReactionType(), "reaction type should be thumb-down");
                  context.assertEquals("User Id 1", userReaction.getDisplayName(), "Display Name should be User Id 1");
                  userReaction = userReactions.get(1);
                  context.assertEquals("user-id-0", userReaction.getUserId(), "reaction of user-id-0 should be in second position");
                  context.assertEquals("ENSEIGNANT", userReaction.getProfile(), "reaction profile should be ENSEIGNANT");
                  context.assertEquals("thumb-up", userReaction.getReactionType(), "reaction type should be thumb-up");
                  context.assertEquals("User Id 0", userReaction.getDisplayName(), "Display Name should be User Id 0");
                  userReaction = userReactions.get(2);
                  context.assertEquals("user-id-2", userReaction.getUserId(), "reaction of user-id-2 should be in");
                  context.assertEquals("PERSRELELEVE", userReaction.getProfile(), "reaction profile should be PERSRELELEVE");
                  context.assertEquals("thumb-down", userReaction.getReactionType(), "reaction type should be thumb-down");
                  context.assertEquals("User Id 2", userReaction.getDisplayName(), "Display Name should be User Id 2");
                  async.complete();
              });
  }

    @Test
    public void testGetReactionDetailsWithPagination(final TestContext context) {
        final Async async = context.async();
        reactionService.getReactionDetails("mod0", "rt0", "r-id-0", 3, 1)
                .onFailure(context::fail)
                .onSuccess(reactionDetails -> {
                    context.assertNotNull(reactionDetails, "getReactionDetails should never return null");
                    context.assertNotNull(reactionDetails.getReactionCounters(), "reaction counters should never return null");
                    final Map<String, Integer> reactionCounterByType = reactionDetails.getReactionCounters().getCountByType();
                    context.assertEquals(2, reactionCounterByType.size(), "there should be 2 types of reactions for this resource");
                    context.assertEquals(1, reactionCounterByType.get("thumb-up"), "there should be 1 reaction of type thumb-up");
                    context.assertEquals(2, reactionCounterByType.get("thumb-down"), "there should be 2 reactions of type thumb-down");
                    final List<UserReaction> userReactions = reactionDetails.getUserReactions();
                    context.assertNotNull(userReactions, "list of user reactions should never return null");
                    context.assertEquals(1, userReactions.size(), "there should be 1 reactions with this pagination parameters");
                    UserReaction userReaction = userReactions.get(0);
                    context.assertEquals("user-id-2", userReaction.getUserId(), "reaction of user-id-2 should be in");
                    context.assertEquals("PERSRELELEVE", userReaction.getProfile(), "reaction profile should be PERSRELELEVE");
                    context.assertEquals("thumb-down", userReaction.getReactionType(), "reaction type should be thumb-down");
                    context.assertEquals("User Id 2", userReaction.getDisplayName(), "Display Name should be User Id 2");
                    async.complete();
                });
    }

  @Test
  public void testUpsertReaction(final TestContext context) {
    final Async async = context.async();
    final UserInfos userInfos = new UserInfos();
    userInfos.setUserId("user-id");
    userInfos.setFirstName("first-name");
    userInfos.setLastName("last-name");
    userInfos.setType("PERSRELELEVE");

    final UserInfos userInfos2 = new UserInfos();
    userInfos2.setUserId("user-id2");
    userInfos2.setFirstName("first-name-2");
    userInfos2.setLastName("last-name-2");
    userInfos2.setType("ENSEIGNANT");

    final UserInfos userInfos3 = new UserInfos();
    userInfos3.setUserId("user-id3");
    userInfos3.setFirstName("first-name-3");
    userInfos3.setLastName("last-name-3");
    userInfos3.setType("PERSEDUCNAT");

    final int page = 1;
    final int size = 100;

    // User 1 saves their first reaction
    reactionService.upsertReaction("mod-upsert", "rt-upsert", "r-id-upsert-0", userInfos, "reaction-type-1")
    .compose(e -> reactionService.getReactionDetails("mod-upsert", "rt-upsert", "r-id-upsert-0", page, size))
    .onSuccess(reactionDetails -> {
      final int count = reactionDetails.getReactionCounters().getCountByType().get("reaction-type-1");
      context.assertEquals(1, count, "Should have a count of one because we just registered a reaction of this type for this resource");
    })
    // User 1 saves another reaction for the same resource
    .compose(e -> reactionService.upsertReaction("mod-upsert", "rt-upsert", "r-id-upsert-0", userInfos, "reaction-type-2"))
    .compose(e -> reactionService.getReactionDetails("mod-upsert", "rt-upsert", "r-id-upsert-0", page, size))
    .onSuccess(e -> {
      final Map<String, Integer> counts = e.getReactionCounters().getCountByType();
      context.assertEquals(1, counts.size(), "Should have only one entry for this resource because the only two reactions come from the same user so the latest should replace the other one");
      final int count = counts.get("reaction-type-2");
      context.assertEquals(1, count, "Should have a count of one because we just registered a reaction of this type for this resource");
    })
    // Another user registers a reaction for the same resource
    .compose(e -> reactionService.upsertReaction("mod-upsert", "rt-upsert", "r-id-upsert-0", userInfos2, "reaction-type-3"))
    .compose(e -> reactionService.getReactionDetails("mod-upsert", "rt-upsert", "r-id-upsert-0", page, size))
    .onSuccess(e -> {
      final Map<String, Integer> counts = e.getReactionCounters().getCountByType();
      context.assertEquals(2, counts.size(), "Should have 2 entries, one per user who saved a reaction");
      context.assertEquals(1, counts.get("reaction-type-2"), "User 1 previously registered that reaction so it should appear");
      context.assertEquals(1, counts.get("reaction-type-3"), "User 1 previously registered that reaction so it should appear");
    })
    // Yet another user registers a reaction for this resource but of a type which has already been registered
    .compose(e -> reactionService.upsertReaction("mod-upsert", "rt-upsert", "r-id-upsert-0", userInfos3, "reaction-type-3"))
    .compose(e -> reactionService.getReactionDetails("mod-upsert", "rt-upsert", "r-id-upsert-0", page, size))
    .onSuccess(e -> {
      final Map<String, Integer> counts = e.getReactionCounters().getCountByType();
      context.assertEquals(2, counts.size(), "Should have 2 entries, one per type of reaction");
      context.assertEquals(1, counts.get("reaction-type-2"), "User 1 previously registered that reaction so it should appear");
      context.assertEquals(2, counts.get("reaction-type-3"), "User 2 and 3 previously registered that reaction so it should appear");
      async.complete();
    })
    .onFailure(context::fail);
  }

  @Test
  public void testDeleteReaction(final TestContext context) {

      final Async async = context.async();
      final UserInfos userInfos = new UserInfos();
      userInfos.setUserId("user-id");
      userInfos.setFirstName("first-name");
      userInfos.setLastName("last-name");
      userInfos.setType("PERSRELELEVE");

      final UserInfos userInfos2 = new UserInfos();
      userInfos2.setUserId("user-id2");
      userInfos2.setFirstName("first-name-2");
      userInfos2.setLastName("last-name-2");
      userInfos2.setType("ENSEIGNANT");

      final int page = 1;
      final int size = 100;

      // User saves their first reaction
      reactionService.upsertReaction("mod-delete", "rt-delete", "r-id-delete-0", userInfos, "reaction-type-1")
              .compose(e -> reactionService.upsertReaction("mod-delete", "rt-delete", "r-id-delete-0", userInfos2, "reaction-type-1"))
              .compose(e -> reactionService.getReactionDetails("mod-delete", "rt-delete", "r-id-delete-0", page, size))
              .onSuccess(reactionDetails -> {
                  context.assertEquals(2, reactionDetails.getReactionCounters().getAllReactionsCounter(), "Should be a total of two reactions for this resource");
                  context.assertEquals(1, reactionDetails.getReactionCounters().getCountByType().size(), "Should be only one type of reaction");
                  context.assertEquals(2, reactionDetails.getReactionCounters().getCountByType().get("reaction-type-1"), "Should have a count of two for reaction-type-1");
                  context.assertEquals(2, reactionDetails.getUserReactions().size(), "Users' reaction list should contain two reactions");
                  context.assertTrue(reactionDetails.getUserReactions().stream().map(UserReaction::getUserId).collect(Collectors.toSet()).containsAll(Sets.newHashSet("user-id", "user-id2")), "Users' reaction list should contain user-id and user-id2");
              })
              .compose(e -> reactionService.deleteReaction("mod-delete", "rt-delete", "r-id-delete-0", userInfos))
              .compose(e -> reactionService.getReactionDetails("mod-delete", "rt-delete", "r-id-delete-0", page, size))
              .onSuccess(reactionDetails -> {
                  context.assertEquals(1, reactionDetails.getReactionCounters().getAllReactionsCounter(), "Should be a total of one reaction for this resource, after deleting one reaction");
                  context.assertEquals(1, reactionDetails.getReactionCounters().getCountByType().size(), "Should be only one type of reaction");
                  context.assertEquals(1, reactionDetails.getReactionCounters().getCountByType().get("reaction-type-1"), "One reaction of type reaction-type-1 should have been deleted");
                  context.assertEquals(1, reactionDetails.getUserReactions().size(), "List of users' reactions should contain 1 reaction after deletion");
                  context.assertTrue(reactionDetails.getUserReactions().stream().noneMatch(userReaction -> userReaction.getUserId().equals("user-id")), "Reaction of user-id should have been removed from users' reaction list.");
              })
              .compose(e -> reactionService.deleteReaction("mod-delete", "rt-delete", "r-id-delete-0", userInfos2))
              .compose(e -> reactionService.getReactionDetails("mod-delete", "rt-delete", "r-id-delete-0", page, size))
              .onSuccess(reactionDetails -> {
                  context.assertNull(reactionDetails.getReactionCounters(), "Reaction counters should be null, after deleting last reaction");
                  context.assertTrue(reactionDetails.getUserReactions().isEmpty(), "List of users' reactions should be empty after deleting last reaction");
                  async.complete();
              });
  }

  /**
   * Insert fake reaction data.
   * @return when preparation is done
   */
  private static Future<Void> insertMockData() {
    final Promise<Void> promise = Promise.promise();
    final Sql sql = Sql.getInstance();
    final String values = String.join(",", Lists.newArrayList(
            "('mod0', 'rt0', 'r-id-0', 'ENSEIGNANT', 'user-id-0', '2024-01-01', 'thumb-up')",
            "('mod0', 'rt0', 'r-id-0', 'PERSRELELEVE',      'user-id-1', '2024-01-02', 'thumb-down')",
            "('mod0', 'rt0', 'r-id-0', 'PERSRELELEVE',      'user-id-2', '2023-01-02', 'thumb-down')",
            "('mod0', 'rt0', 'r-id-1', 'ENSEIGNANT', 'user-id-0', '2024-02-01', 'love')",
            "('mod1', 'rt1', 'r-id-2', 'PERSRELELEVE', 'user-id-1', '2024-02-02', 'heart')"
    ));
    sql.raw("INSERT into audience.reactions (module, resource_type, resource_id, profile, user_id, reaction_date, reaction_type) VALUES " + values, e -> {
      if ("ok".equals(e.body().getString("status"))) {
        promise.complete();
      } else {
        promise.fail("could.not.insert.reactions.data : " + e.body().getString("message"));
      }
    });
    return promise.future();
  }
}
