package org.entcore.common.audience;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.entcore.common.user.UserInfos;
import org.entcore.common.user.UserUtils;
import org.vertx.java.core.http.RouteMatcher;

import fr.wseduc.webutils.http.BaseController;
import fr.wseduc.webutils.http.Renders;
import fr.wseduc.webutils.request.RequestUtils;
import fr.wseduc.webutils.security.SecuredAction;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonObject;

public class AudienceController extends BaseController {

  private final AudienceAccessFilter accessFilter;
  private final AudienceHelper audienceHelper;
  private final String resourceType;

  public AudienceController(AudienceAccessFilter accessFilter, String appName, String resourceType, Vertx vertx,
      JsonObject config) {
    this.accessFilter = accessFilter;
    this.resourceType = resourceType;
    this.audienceHelper = new AudienceHelper(appName, resourceType, vertx);
  }

  @Override
  public void init(Vertx vertx, JsonObject config, RouteMatcher rm, Map<String, SecuredAction> securedActions) {
    super.init(vertx, config, rm, securedActions);

    get("/reaction/" + resourceType, "getReactionsSummary");
    get("/reaction/" + resourceType + "/:resourceId", "getReactionsDetails");
    post("/reaction/" + resourceType, "upsertReaction");
    put("/reaction/" + resourceType, "upsertReaction");
    delete("/reaction/" + resourceType + "/:resourceId", "deleteReactionForUserAndResource");
  }

  public void deleteReactionForUserAndResource(final HttpServerRequest request) {
    final String resourceId = request.getParam("resourceId");
    final Set<String> resourceIds = new HashSet<>();
    resourceIds.add(resourceId);
    verify(resourceIds, request)
        .onSuccess(user -> audienceHelper.deleteReactionForUserAndResource(resourceId, request, user)
            .onSuccess(e -> Renders.ok(request))
            .onFailure(th -> {
              log.error("Error while deleting reaction for user and resource", th);
            }));
  }

  public void getReactionsSummary(final HttpServerRequest request) {
    final Set<String> resourceIds = RequestUtils.getParamAsSet("resourceId", request);
    verify(resourceIds, request)
        .onSuccess(user -> audienceHelper.getReactionsSummary(resourceIds, request, user));
  }

  public void upsertReaction(final HttpServerRequest request) {
    RequestUtils.bodyToJson(request, data -> {
      final String resourceId = data.getString("id", "");
      final String reactionType = data.getString("reactionType", "");
      final Set<String> resourceIds = new HashSet<>();
      resourceIds.add(resourceId);
      verify(resourceIds, request)
          .onSuccess(user -> audienceHelper.upsertReaction(reactionType, resourceId, request, user));
    });
  }

  public void getReactionsDetails(final HttpServerRequest request) {
    final Set<String> resourceIds = new HashSet<>();
    resourceIds.add(request.getParam("resourceId"));
    verify(resourceIds, request)
        .onSuccess(user -> audienceHelper.getReactionsDetails(resourceIds, request, user));
  }

  /**
   * @param resourceIds Ids of the resources whose access by the user should be
   *                    checked
   * @param request     Incoming request to verify
   * @return Succeeds iff the requesting user if this user can access the
   *         resources, <b>does not complete otherwise</b> but send a response to
   *         the user.
   */
  private Future<UserInfos> verify(
      final Set<String> resourceIds,
      final HttpServerRequest request) {
    final Promise<UserInfos> promise = Promise.promise();
    UserUtils.getAuthenticatedUserInfos(eb, request).onSuccess(user -> {
      accessFilter.canAccess(user, resourceIds).onSuccess(canAccess -> {
        if (canAccess) {
          promise.complete(user);
        } else {
          promise.fail("user.cannot.access");
          Renders.forbidden(request);
        }
      }).onFailure(th -> {
        log.error("Error while fetching the user credentials to access the resources", th);
        promise.fail(th);
        Renders.renderError(request);
      });
    });
    return promise.future();
  }
}
