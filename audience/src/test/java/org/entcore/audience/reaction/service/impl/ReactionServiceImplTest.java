package org.entcore.audience.reaction.service.impl;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.entcore.audience.reaction.dao.impl.ReactionDaoImpl;
import org.entcore.audience.reaction.model.ReactionsSummary;
import org.entcore.audience.reaction.service.ReactionService;
import org.entcore.common.sql.Sql;
import org.entcore.common.user.UserInfos;
import org.entcore.test.TestHelper;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.testcontainers.containers.PostgreSQLContainer;

import java.util.Map;
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
    userInfos.setFirstName("last-name");
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
    userInfos.setFirstName("last-name");
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
    userInfos.setFirstName("last-name");
    reactionService.getReactionsSummary("mod0", "rt0", Sets.newHashSet("r-id-0"), userInfos)
        .onFailure(context::fail)
        .onSuccess(summary -> {
          context.assertNotNull(summary, "getReactionsSummary should never return null");
          context.assertNotNull(summary.getReactionsByResource(), "getReactionsSummary should never return null");
          context.assertEquals(1, summary.getReactionsByResource().size(), "getReactionsSummary should return a counter for an existing resource");
          final ReactionsSummary.ReactionsSummaryForResource resSummary = summary.getReactionsByResource().get("r-id-0");
          context.assertNotNull(resSummary, "The summary of an existing resource should not be null");
          final Map<String, Integer> counts = resSummary.getCountByType();
          context.assertEquals(2, counts.size(), "There should be two types of reactions for this resource");
          context.assertEquals(1, counts.get("thumb-up"), "There should be only one thumb up (cf. prepareData method)");
          context.assertEquals(2, counts.get("thumb-down"), "There should be 3 thumbs down (cf. prepareData method)");
          async.complete();
        });
  }

  @Test
  public void testGetReactionsSummaryWithMultipleExistingResources(final TestContext context) {
    final Async async = context.async();
    final UserInfos userInfos = new UserInfos();
    userInfos.setUserId("user-id");
    userInfos.setFirstName("first-name");
    userInfos.setFirstName("last-name");
    reactionService.getReactionsSummary("mod0", "rt0", Sets.newHashSet("r-id-0", "r-id-1"), userInfos)
        .onFailure(context::fail)
        .onSuccess(summary -> {
          context.assertNotNull(summary, "getReactionsSummary should never return null");
          context.assertNotNull(summary.getReactionsByResource(), "getReactionsSummary should never return null");
          context.assertEquals(2, summary.getReactionsByResource().size(), "getReactionsSummary should return counters for existing resources");

          ReactionsSummary.ReactionsSummaryForResource resSummary = summary.getReactionsByResource().get("r-id-0");
          Map<String, Integer> counts = resSummary.getCountByType();
          context.assertEquals(2, counts.size(), "There should be two types of reactions for this resource");
          context.assertEquals(1, counts.get("thumb-up"), "There should be only one thumb up (cf. prepareData method)");
          context.assertEquals(2, counts.get("thumb-down"), "There should be 3 thumbs down (cf. prepareData method)");

          resSummary = summary.getReactionsByResource().get("r-id-1");
          counts = resSummary.getCountByType();
          context.assertEquals(1, counts.size(), "There should be two types of reactions for this resource");
          context.assertEquals(1, counts.get("love"), "There should be only one love (cf. prepareData method)");

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

          ReactionsSummary.ReactionsSummaryForResource resSummary = summary.getReactionsByResource().get("r-id-0");
          Map<String, Integer> counts = resSummary.getCountByType();
          context.assertEquals(2, counts.size(), "There should be two types of reactions for this resource");
          context.assertEquals(1, counts.get("thumb-up"), "There should be only one thumb up (cf. prepareData method)");
          context.assertEquals(2, counts.get("thumb-down"), "There should be 3 thumbs down (cf. prepareData method)");

          resSummary = summary.getReactionsByResource().get("r-id-1");
          counts = resSummary.getCountByType();
          context.assertEquals(1, counts.size(), "There should be two types of reactions for this resource");
          context.assertEquals(1, counts.get("love"), "There should be only one love (cf. prepareData method)");

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
