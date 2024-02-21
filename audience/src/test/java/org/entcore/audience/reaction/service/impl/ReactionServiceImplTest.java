package org.entcore.audience.reaction.service.impl;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.entcore.audience.reaction.dao.impl.ReactionDaoImpl;
import org.entcore.audience.reaction.model.ReactionCounters;
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
  public static void setUp(TestContext context) throws Exception {
    final Async async = context.async();
    test.database().initPostgreSQL(context, pgContainer, schema).handler(e -> {
      if(e.succeeded()) {
        insertMockData().onFailure(th -> context.fail(th)).onSuccess(s -> async.complete());
      } else {
        context.fail(e.cause());
      }
    });
    final ReactionDaoImpl reactionDao = new ReactionDaoImpl(Sql.getInstance());
    reactionService = new ReactionServiceImpl(reactionDao);
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
  public void testGetReactionDetails(final TestContext context) {
      final Async async = context.async();

      reactionService.getReactionDetails("mod0", "rt0", "r-id-0", 1, 100)
              .onFailure(context::fail)
              .onSuccess(reactionDetails -> {
                  context.assertNotNull(reactionDetails, "");
                  final Map<String, Integer> reactionCounterByType = reactionDetails.getReactionCounters().getCountByType();
                  context.assertEquals(2, reactionCounterByType.size(), "");
                  context.assertEquals(1, reactionCounterByType.get("thumb-up"), "");
                  context.assertEquals(2, reactionCounterByType.get("thumb-down"), "");
                  final List<UserReaction> userReactions = reactionDetails.getUserReactions();
                  context.assertNotNull(userReactions, "");
                  context.assertEquals(3, userReactions.size(), "");

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

  /**
   * Insert fake reaction data.
   * @return when preparation is done
   */
  private static Future<Void> insertMockData() {
    final Promise<Void> promise = Promise.promise();
    final Sql sql = Sql.getInstance();
    final String values = Lists.newArrayList(
      "('mod0', 'rt0', 'r-id-0', 'ENSEIGNANT', 'user-id-0', '2024-01-01', 'thumb-up')",
                "('mod0', 'rt0', 'r-id-0', 'PERSRELELEVE',      'user-id-1', '2024-01-02', 'thumb-down')",
                "('mod0', 'rt0', 'r-id-0', 'PERSRELELEVE',      'user-id-2', '2023-01-02', 'thumb-down')",
                "('mod0', 'rt0', 'r-id-1', 'ENSEIGNANT', 'user-id-0', '2024-02-01', 'love')",
                "('mod1', 'rt1', 'r-id-2', 'PERSRELELEVE', 'user-id-1', '2024-02-02', 'heart')"
    ).stream().collect(Collectors.joining(","));
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
