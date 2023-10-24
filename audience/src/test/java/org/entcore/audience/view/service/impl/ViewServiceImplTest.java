package org.entcore.audience.view.service.impl;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.entcore.audience.view.dao.impl.ViewDaoImpl;
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
    // Activate this test only if "LONG_TESTS" is true
    if(!"true".equalsIgnoreCase(System.getenv("LONG_TESTS"))) {
      return;
    }
    final Async async = context.async();
    final UserInfos userInfos = new UserInfos();
    userInfos.setUserId("user-id");
    userInfos.setFirstName("first-name");
    userInfos.setLastName("last-name");
    userInfos.setType("PERSRELELEVE");
    final UserInfos userInfos2 = new UserInfos();
    userInfos2.setUserId("user-id-2");
    userInfos2.setFirstName("first-name-2");
    userInfos2.setLastName("last-name-2");
    userInfos2.setType("PERSRELELEVE");
    // Register a view for a user
    viewService.registerView("mod-view", "rt-view", "r-id-0", userInfos)
    .compose(e -> viewService.getViewCounters("mod-view", "rt-view", Collections.singleton("r-id-0")))
    .onSuccess(counts -> {
      context.assertEquals(1, counts.size(), "should only have the view recently registered");
      context.assertEquals(1, counts.get("r-id-0"));
    })
    // Try to register a new view but it shouldn't change the counter
    .compose(e -> viewService.registerView("mod-view", "rt-view", "r-id-0", userInfos))
    .compose(e -> viewService.getViewCounters("mod-view", "rt-view", Collections.singleton("r-id-0")))
    .onSuccess(counts -> {
      context.assertEquals(1, counts.size());
      context.assertEquals(1, counts.get("r-id-0"), "should not have been incremented because it has been less than a minute between the 2 views");
    })
    // Wait a bit...
    .compose(e -> {
      final Promise<Void> p = Promise.promise();
      test.vertx().setTimer(60001, timeout -> p.complete());
      return p.future();
    })
    // Register a new view but thi one should count
    .compose(e -> viewService.registerView("mod-view", "rt-view", "r-id-0", userInfos))
    .compose(e -> viewService.getViewCounters("mod-view", "rt-view", Collections.singleton("r-id-0")))
    .onSuccess(counts -> {
      context.assertEquals(1, counts.size());
      context.assertEquals(2, counts.get("r-id-0"), "should have been incremented because it has been more than a minute between the 2 views");
    })
    // Register a new view, for the same resource, but for another user
    .compose(e -> viewService.registerView("mod-view", "rt-view", "r-id-0", userInfos2))
    .compose(e -> viewService.getViewCounters("mod-view", "rt-view", Collections.singleton("r-id-0")))
    .onSuccess(counts -> {
      context.assertEquals(1, counts.size());
      context.assertEquals(3, counts.get("r-id-0"), "should have been incremented because another user viewed the resource");
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
    .compose(e -> viewService.getViewDetails("mod-view-details", "rt-details", "rsc-details-0"))
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

  @Test
  public void testMergeUserViews(TestContext context) {
    Async async = context.async();
    final String module = "mod-view-merge";
    final String resourceType = "r-type-merge";
    final String resourceId1 = "r-id-merge-1";
    final String resourceId2 = "r-id-merge-2";
    final UserInfos userInfos1 = new UserInfos();
    userInfos1.setUserId("parent-1");
    userInfos1.setFirstName("first-name-1");
    userInfos1.setLastName("last-name-1");
    userInfos1.setType("PERSRELELEVE");
    final UserInfos userInfos2 = new UserInfos();
    userInfos2.setUserId("parent-2");
    userInfos2.setFirstName("first-name-2");
    userInfos2.setLastName("last-name-2");
    userInfos2.setType("PERSRELELEVE");
    viewService.registerView(module, resourceType, resourceId1, userInfos1)
            .compose(e -> viewService.registerView(module, resourceType, resourceId1, userInfos2))
            .compose(e -> viewService.registerView(module, resourceType, resourceId2, userInfos1))
            .compose(e -> viewService.registerView(module, resourceType, resourceId2, userInfos2))
            .compose(e -> viewService.getViewDetails(module, resourceType, resourceId1))
            // check resource 1 view details before merge
            .onSuccess(viewDetails -> {
              context.assertEquals(2, viewDetails.getViewsCounter(), "should be a total of two views, one for each user");
              context.assertEquals(2, viewDetails.getUniqueViewsCounter(), "should be a total of two unique views, one for each user");
            })
            // check resource 2 view details before merge
		    .compose(e -> viewService.getViewDetails(module, resourceType, resourceId2))
		    .onSuccess(viewDetails -> {
			    context.assertEquals(2, viewDetails.getViewsCounter(), "should be a total of two views, one for each user");
			    context.assertEquals(2, viewDetails.getUniqueViewsCounter(), "should be a total of two unique views, one for each user");
		    })
            // merging all views for user 1 and user 2
            .compose(e -> viewService.mergeUserViews(userInfos1.getUserId(), userInfos2.getUserId()))
            // check resource 1 view details after merge
            .compose(e -> viewService.getViewDetails(module, resourceType, resourceId1))
            .onSuccess(viewDetails -> {
              context.assertEquals(2, viewDetails.getViewsCounter(), "should still be a total of two views after merge");
              context.assertEquals(1, viewDetails.getUniqueViewsCounter(), "should now be one unique view after merging user 1 and user 2 views");
            })
            // check resource 2 view details after merge
            .compose(e -> viewService.getViewDetails(module, resourceType, resourceId2))
            .onSuccess(viewDetails -> {
              context.assertEquals(2, viewDetails.getViewsCounter(), "should still be a total of two views after merge");
              context.assertEquals(1, viewDetails.getUniqueViewsCounter(), "should now be one unique view after merging user 1 and user 2 views");
              async.complete();
            })
            .onFailure(context::fail);
  }

  @Test
  public void testDeleteAllViewsOfResources(TestContext context) {
    Async async = context.async();
    final String module = "mod-view-delete";
    final String resourceType = "r-type-delete";
    final String resourceId1 = "r-id-delete-1";
    final String resourceId2 = "r-id-delete-2";
    final UserInfos userInfos1 = new UserInfos();
    userInfos1.setUserId("parent-1");
    userInfos1.setFirstName("first-name-1");
    userInfos1.setLastName("last-name-1");
    userInfos1.setType("PERSRELELEVE");
    final UserInfos userInfos2 = new UserInfos();
    userInfos2.setUserId("parent-2");
    userInfos2.setFirstName("first-name-2");
    userInfos2.setLastName("last-name-2");
    userInfos2.setType("PERSRELELEVE");
    viewService.registerView(module, resourceType, resourceId1, userInfos1)
            .compose(e -> viewService.registerView(module, resourceType, resourceId1, userInfos2))
            .compose(e -> viewService.registerView(module, resourceType, resourceId2, userInfos1))
            .compose(e -> viewService.registerView(module, resourceType, resourceId2, userInfos2))
            .compose(e -> viewService.getViewCounters(module, resourceType, Sets.newHashSet(resourceId1, resourceId2)))
            // check views on resource 1 and 2
            .onSuccess(counts -> {
              context.assertEquals(2, counts.size(), "should be a total of two views per resource id");
              context.assertEquals(2, counts.get(resourceId1), "should be a total of two views for resource 1");
              context.assertEquals(2, counts.get(resourceId2), "should be a total of two views for resource 2");
            })
            // delete all views on resource 1 and 2
            .compose(e -> viewService.deleteAllViewsOfResources(Sets.newHashSet(resourceId1, resourceId2)))
            // check views on resource 1 and 2
            .compose(e -> viewService.getViewCounters(module, resourceType, Sets.newHashSet(resourceId1, resourceId2)))
            .onSuccess(counts -> {
              context.assertEquals(0, counts.get(resourceId1), "view count of resource 1 should be 0 after purge");
              context.assertEquals(0, counts.get(resourceId2), "view count of resource 2 should be 0 after purge");
              async.complete();
            })
            .onFailure(context::fail);
  }

  /**
   * Insert views data.
   * @return when preparation is done
   */
  public static Future<Void> insertMockData() {
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
