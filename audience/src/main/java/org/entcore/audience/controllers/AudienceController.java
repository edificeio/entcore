package org.entcore.audience.controllers;

import java.util.HashSet;
import java.util.Set;

import org.entcore.audience.services.AudienceAccessFilter;
import org.entcore.audience.services.impl.EventBusAudienceAccessFilter;
import org.entcore.common.audience.AudienceHelper;
import org.entcore.common.user.UserInfos;
import org.entcore.common.user.UserUtils;

import fr.wseduc.rs.Get;
import fr.wseduc.rs.Post;
import fr.wseduc.rs.Put;
import fr.wseduc.rs.Delete;
import fr.wseduc.security.ActionType;
import fr.wseduc.security.SecuredAction;
import fr.wseduc.webutils.http.BaseController;
import fr.wseduc.webutils.http.Renders;
import fr.wseduc.webutils.request.RequestUtils;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonObject;

public class AudienceController extends BaseController {

  private final AudienceHelper audienceHelper;

  private final AudienceAccessFilter audienceAccessFilter;

  public AudienceController(final Vertx vertx, final JsonObject config) {
    // todo implement access filter creation
    this.audienceHelper = new AudienceHelper(vertx);
    this.audienceAccessFilter = new EventBusAudienceAccessFilter(vertx);
  }


  @Get("/reactions/:appName/:resourceType")
  @SecuredAction(value = "", type = ActionType.AUTHENTICATED)
  public void getReactionsSummary(final HttpServerRequest request) {
    final Set<String> resourceIds = RequestUtils.getParamAsSet("resourceId", request);
    verify(request.getParam("appName"), request.getParam("resourceType"), resourceIds, request)
        .onSuccess(user -> audienceHelper.getReactionsSummary(resourceIds, request, user));
  }

  @Get("/reactions/:appName/:resourceType/:resourceId")
  @SecuredAction(value = "", type = ActionType.AUTHENTICATED)
  public void getReactionsDetails(final HttpServerRequest request) {
    final Set<String> resourceIds = new HashSet<>();
    resourceIds.add(request.getParam("resourceId"));
    verify(request.getParam("appName"), request.getParam("resourceType"), resourceIds, request)
        .onSuccess(user -> audienceHelper.getReactionsDetails(resourceIds, request, user));
  }

  @Post("/reactions/:appName/:resourceType")
  @SecuredAction(value = "", type = ActionType.AUTHENTICATED)
  public void createReaction(final HttpServerRequest request) {
    doUpsertReaction(request);
  }

  @Put("/reactions/:appName/:resourceType")
  @SecuredAction(value = "", type = ActionType.AUTHENTICATED)
  public void updateReaction(final HttpServerRequest request) {
    doUpsertReaction(request);
  }

  @Delete("/reactions/:appName/:resourceType/:resourceId")
  @SecuredAction(value = "", type = ActionType.AUTHENTICATED)
  public void deleteReaction(final HttpServerRequest request) {
    final String resourceId = request.getParam("resourceId");
    final Set<String> resourceIds = new HashSet<>();
    resourceIds.add(resourceId);
    verify(request.getParam("appName"), request.getParam("resourceType"), resourceIds, request)
        .onSuccess(user -> audienceHelper.deleteReactionForUserAndResource(resourceId, request, user)
            .onSuccess(e -> Renders.ok(request))
            .onFailure(th -> {
              Renders.log.error("Error while deleting reaction for user and resource", th);
            }));
  }


  public void doUpsertReaction(final HttpServerRequest request) {
    RequestUtils.bodyToJson(request, data -> {
      final String resourceId = data.getString("id", "");
      final String reactionType = data.getString("reactionType", "");
      final Set<String> resourceIds = new HashSet<>();
      resourceIds.add(resourceId);
      verify(request.getParam("appName"), request.getParam("resourceType"), resourceIds, request)
          .onSuccess(user -> audienceHelper.upsertReaction(reactionType, resourceId, request, user));
    });
  }

  /**
   * @param appName Name of the application which owns the resources to check
   * @param resourceType Type of the resources to check
   * @param resourceIds Ids of the resources whose access by the user should be
   *                    checked
   * @param request     Incoming request to verify
   * @return Succeeds iff the requesting user if this user can access the
   *         resources, <b>does not complete otherwise</b> but send a response to
   *         the user.
   */
  private Future<UserInfos> verify(
      final String appName,
      final String resourceType,
      final Set<String> resourceIds,
      final HttpServerRequest request) {
    final Promise<UserInfos> promise = Promise.promise();
    UserUtils.getAuthenticatedUserInfos(eb, request).onSuccess(user -> {
      audienceAccessFilter.canAccess( appName, resourceType, user, resourceIds).onSuccess(canAccess -> {
        if (canAccess) {
          promise.complete(user);
        } else {
          promise.fail("user.cannot.access");
          Renders.forbidden(request);
        }
      }).onFailure(th -> {
        Renders.log.error("Error while fetching the user credentials to access the resources", th);
        promise.fail(th);
        Renders.renderError(request);
      });
    });
    return promise.future();
  }
}
