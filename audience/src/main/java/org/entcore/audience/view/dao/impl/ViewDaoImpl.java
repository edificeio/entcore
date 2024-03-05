package org.entcore.audience.view.dao.impl;

import fr.wseduc.webutils.Either;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.entcore.audience.view.dao.ViewDao;
import org.entcore.audience.view.model.ResourceViewCounter;
import org.entcore.audience.view.model.ResourceViewDetails;
import org.entcore.audience.view.model.ViewsCounterPerProfile;
import org.entcore.common.sql.ISql;
import org.entcore.common.sql.Sql;
import org.entcore.common.sql.SqlResult;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class ViewDaoImpl implements ViewDao {

  private final ISql sql;

  public ViewDaoImpl(ISql sql) {
    this.sql = sql;
  }

  @Override
  public Future<Void> registerView(String module, String resourceType, String resourceId, String userId, String type) {
    Promise<Void> promise = Promise.promise();

    JsonArray params = new JsonArray();
    params.add(module);
    params.add(resourceType);
    params.add(resourceId);
    params.add(type);
    params.add(userId);
    params.add(LocalDateTime.now().toString());

    final String query = "insert into audience.views (module, resource_type, resource_id, profile, user_id, last_view, counter) " +
            "values (?, ?, ?, ?, ?, ?, 1) " +
            "on conflict on constraint views_unique_constraint " +
            "do update " +
            "set counter = views.counter + 1, " +
            "last_view = excluded.last_view " +
            "where date_trunc('minute', views.last_view) <> date_trunc('minute', excluded.last_view);";

    sql.prepared(query, params, results -> {
      final Either<String, JsonArray> validatedResult = SqlResult.validResults(results);
      if (validatedResult.isRight()) {
        promise.complete();
      } else {
        promise.fail(validatedResult.left().getValue());
      }
    });
    return promise.future();
  }

  @Override
  public Future<List<ResourceViewCounter>> getCounts(String module, String resourceType, Set<String> resourceIds) {
    final Promise<List<ResourceViewCounter>> promise = Promise.promise();
    if(CollectionUtils.isEmpty(resourceIds)) {
      promise.complete(Collections.emptyList());
    } else {
      final JsonArray params = new JsonArray();
      params.add(module);
      params.add(resourceType);

      String resourceIdsPlaceholder = Sql.preparePlaceholderAndParamsForInClause(resourceIds, params);

      final String query = "select resource_id, sum(counter) as nb_views " +
              "from audience.views " +
              "where module = ? " +
              "and resource_type = ? " +
              "and resource_id in " + resourceIdsPlaceholder + " " +
              "group by resource_id ";

      sql.prepared(query, params, results -> {
        final Either<String, JsonArray> validatedResult = SqlResult.validGroupedResults(results);
        if (validatedResult.isRight()) {
          final List<ResourceViewCounter> views = validatedResult.right().getValue().stream()
                  .map(e -> (JsonObject)e)
                  .map(entry -> new ResourceViewCounter(entry.getString("resource_id"), entry.getInteger("nb_views")))
                  .collect(Collectors.toList());
          promise.complete(views);
        } else {
          promise.fail(validatedResult.left().getValue());
        }
      });
    }
    return promise.future();
  }

  @Override
  public Future<ResourceViewDetails> getViewDetails(String module, String resourceType, String resourceId) {
    final Promise<ResourceViewDetails> promise = Promise.promise();
    if(StringUtils.isEmpty(resourceId)) {
      promise.fail("resourceId.mandatory");
    } else {
      final JsonArray params = new JsonArray();
      params.add(module);
      params.add(resourceType);
      params.add(resourceId);

      final String query = "SELECT profile, count(*) as nb_unique_views, sum(counter) as nb_views " +
              "FROM audience.views " +
              "WHERE module = ? and resource_type = ? and resource_id = ? " +
              "GROUP BY profile";
      sql.prepared(query, params, results -> {
        final Either<String, JsonArray> validatedResult = SqlResult.validGroupedResults(results);
        if (validatedResult.isRight()) {
          final List<ViewsCounterPerProfile> uniqueViewsPerProfile = new ArrayList<>();
          int nbViews = 0;
          int nbUniqueViews = 0;
          final JsonArray rows = validatedResult.right().getValue();
          for (Object row : rows) {
            final JsonObject e = (JsonObject)row;
            uniqueViewsPerProfile.add(new ViewsCounterPerProfile(e.getString("profile"), e.getInteger("nb_unique_views")));
            nbViews += e.getInteger("nb_views", 0);
            nbUniqueViews += e.getInteger("nb_unique_views", 0);
          }
          promise.complete(new ResourceViewDetails(
                  nbViews, nbUniqueViews, uniqueViewsPerProfile
          ));
        } else {
          promise.fail(validatedResult.left().getValue());
        }
      });
    }
    return promise.future();
  }

  @Override
  public Future<Void> mergeUserViews(String keptUserId, String deletedUserId) {
	  Promise<Void> promise = Promise.promise();
	  JsonArray params = new JsonArray();
      params.add(keptUserId);
      params.add(deletedUserId);
      params.add(keptUserId);
      params.add(deletedUserId);

      String query = "BEGIN;" +
              "WITH merged_view AS ( " +
                "SELECT module, resource_type, resource_id, SUM(counter) as count_sum " +
                "FROM audience.views " +
                "WHERE user_id in (?, ?) " +
                "GROUP BY module, resource_type, resource_id " +
              ") " +
              "UPDATE audience.views AS target " +
              "SET counter = merged_view.count_sum " +
              "FROM merged_view " +
              "WHERE target.module = merged_view.module " +
              "AND target.resource_type = merged_view.resource_type " +
              "AND target.resource_id = merged_view.resource_id " +
              "AND target.user_id = ? ;" +
              "DELETE FROM audience.views WHERE user_id = ? ;" +
              "COMMIT;";

      sql.prepared(query, params, results -> {
        Either<String, JsonArray> validatedResults = SqlResult.validResults(results);
        if (validatedResults.isRight()) {
          promise.complete();
        } else {
          promise.fail(validatedResults.left().getValue());
        }
      });
      return promise.future();
  }

  @Override
  public Future<Void> deleteAllViewsOfResources(Set<String> resourceIds) {
    Promise<Void> promise = Promise.promise();
    JsonArray params = new JsonArray();

    String resourceIdsPlaceholder = Sql.preparePlaceholderAndParamsForInClause(resourceIds, params);

    String query = "delete from audience.views " +
            "where resource_id in " + resourceIdsPlaceholder;

    sql.prepared(query, params, results -> {
      Either<String, JsonArray> validatedResults = SqlResult.validResults(results);
      if (validatedResults.isRight()) {
        promise.complete();
      } else {
        promise.fail(validatedResults.left().getValue());
      }
    });

    return promise.future();
  }
}
