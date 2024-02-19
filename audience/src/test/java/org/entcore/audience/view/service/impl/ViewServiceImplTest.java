package org.entcore.audience.view.service.impl;

import com.google.common.collect.Lists;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.entcore.audience.view.dao.ViewDaoImpl;
import org.entcore.audience.view.model.ResourceViewCounter;
import org.entcore.audience.view.model.ViewsCounterPerProfile;
import org.entcore.audience.view.service.ViewService;
import org.entcore.common.sql.Sql;
import org.entcore.common.user.UserInfos;
import org.entcore.test.TestHelper;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.testcontainers.containers.PostgreSQLContainer;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@RunWith(VertxUnitRunner.class)
public class ViewServiceImplTest {
  private static ViewService viewService;

  private static final TestHelper test = TestHelper.helper();

  @ClassRule
  public static PostgreSQLContainer<?> pgContainer = test.database().createPostgreSQLContainer()
      .withInitScript("initAudience.sql")
      .withReuse(true);
  private static final String schema = "audience";

  @BeforeClass
  public static void setUp(TestContext context) throws Exception {
    test.database().initPostgreSQL(context, pgContainer, schema);
    final ViewDaoImpl viewDao = new ViewDaoImpl(Sql.getInstance());
    viewService = new ViewServiceImpl(viewDao);
  }

  @Test
  public void testViewResource(final TestContext context) {
    final Async async = context.async();
    final UserInfos userInfos = new UserInfos();
    userInfos.setUserId("user-id");
    userInfos.setFirstName("first-name");
    userInfos.setLastName("last-name");
    userInfos.setType("PERSRELELEVE");
    // Register a view for a user
    viewService.registerView("mod-view", "rt-view", "r-id-0", userInfos)
    .compose(e -> viewService.getViewCounts("mod-view", "rt-view", Collections.singleton("r-id-0"), userInfos))
    .onSuccess(counts -> {
      context.assertEquals(1, counts.size(), "should only have the view recently registered");
      final ResourceViewCounter count = counts.get(0);
      context.assertEquals("r-id-0", count.getResourceId());
      context.assertEquals(1, count.getViewCounter());
    })
    // Try to register a new view but it shouldn't change the counter
    .compose(e -> viewService.registerView("mod-view", "rt-view", "r-id-0", userInfos))
    .compose(e -> viewService.getViewCounts("mod-view", "rt-view", Collections.singleton("r-id-0"), userInfos))
    .onSuccess(counts -> {
      context.assertEquals(1, counts.size());
      final ResourceViewCounter count = counts.get(0);
      context.assertEquals("r-id-0", count.getResourceId());
      context.assertEquals(1, count.getViewCounter(), "should not have been incremented because it has been less than a minute between the 2 views");
    })
    // Wait a bit...
    .compose(e -> {
      final Promise<Void> p = Promise.promise();
      test.vertx().setTimer(60001, timeout -> p.complete());
      return p.future();
    })
    // Register a new view but thi one should count
    .compose(e -> viewService.registerView("mod-view", "rt-view", "r-id-0", userInfos))
    .compose(e -> viewService.getViewCounts("mod-view", "rt-view", Collections.singleton("r-id-0"), userInfos))
    .onSuccess(counts -> {
      context.assertEquals(1, counts.size());
      final ResourceViewCounter count = counts.get(0);
      context.assertEquals("r-id-0", count.getResourceId());
      context.assertEquals(2, count.getViewCounter(), "should have been incremented because it has been more than a minute between the 2 views");
      async.complete();
    })
    .onFailure(context::fail);
  }

  /**
   * <h1>Goal</h1>
   * <p>Verify that view details are correctly calculated</p>
   * <h1>Steps</h1>
   * <ol>
   *   <li>Insert in the database 3 view lines corresponding to 3 users as follows :
   *   <ul>
   *     <li>user 1 with profile ENSEIGNANT, has viewed the resource once</li>
   *     <li>user 2 with profile PERSEDUCNAT, has viewed the resource twice</li>
   *     <li>user 3 with profile PERSEDUCNAT, has viewed the resource 4 times</li>
   *   </ul>
   *   </li>
   * </ol>
   * @param context Test context
   */
  @Test
  public void testViewDetails(final TestContext context) {
    final Async async = context.async();
    final UserInfos userInfos = new UserInfos();
    userInfos.setUserId("user-id");
    userInfos.setFirstName("first-name");
    userInfos.setLastName("last-name");
    userInfos.setType("PERSRELELEVE");
    // Register a view for a user
    insertMockData()
    .compose(e -> viewService.getViewDetails("mod-view-details", "rt-details", "rsc-details-0", userInfos))
    .onSuccess(details -> {
      context.assertEquals(3, details.getUniqueViewsCounter(), "should only have 3 unique views because 3 users saw it");
      context.assertEquals(7, details.getViewsCounter(), "should only have 7 views because user-1 saw it once, user-2 twice and user-3 4 times");
      final List<ViewsCounterPerProfile> uniqueViewsPerProfile = details.getUniqueViewsPerProfile();
      context.assertNotNull(uniqueViewsPerProfile, "Should not get back a null break down of views per profile");
      context.assertEquals(2, uniqueViewsPerProfile.size(), "Should only have 2 entries because user-1 and user-2 have the same profile and user-3 has a different one");
      for (ViewsCounterPerProfile viewsCounterPerProfile : uniqueViewsPerProfile) {
        final String profile = viewsCounterPerProfile.getProfile();
        if("ENSEIGNANT".equals(profile)) {
          context.assertEquals(1, viewsCounterPerProfile.getCounter(), "Only the teacher has seen it");
        } else if("PERSEDUCNAT".equals(profile)) {
          context.assertEquals(2, viewsCounterPerProfile.getCounter(), "user-& and user-2 have seen it");
        } else {
          context.fail(profile + ".unexpected");
        }
      }
      async.complete();
    })
    .onFailure(context::fail);
  }

  /**
   * Insert views data.
   * @return when preparation is done
   */
  private static Future<Void> insertMockData() {
    final Promise<Void> promise = Promise.promise();
    final Sql sql = Sql.getInstance();
    final String values = Lists.newArrayList(
        "('mod-view-details', 'rt-details', 'rsc-details-0', 'ENSEIGNANT', 'user-id-1', '2024-01-01', 1)",
        "('mod-view-details', 'rt-details', 'rsc-details-0', 'PERSEDUCNAT',      'user-id-2', '2024-01-02', 2)",
        "('mod-view-details', 'rt-details', 'rsc-details-0', 'PERSEDUCNAT',      'user-id-3', '2023-01-02', 4)"
    ).stream().collect(Collectors.joining(","));
    sql.raw("INSERT into audience.views (module, resource_type, resource_id, profile, user_id, last_view, counter) VALUES " + values, e -> {
      if ("ok".equals(e.body().getString("status"))) {
        promise.complete();
      } else {
        promise.fail("could.not.insert.views.data : " + e.body().getString("message"));
      }
    });
    return promise.future();
  }
}
