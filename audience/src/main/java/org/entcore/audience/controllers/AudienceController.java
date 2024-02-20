package org.entcore.audience.controllers;

import fr.wseduc.rs.Delete;
import fr.wseduc.rs.Get;
import fr.wseduc.rs.Post;
import fr.wseduc.rs.Put;
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
import org.entcore.audience.reaction.model.ReactionType;
import org.entcore.audience.reaction.service.ReactionService;
import org.entcore.audience.services.AudienceAccessFilter;
import org.entcore.audience.view.service.ViewService;
import org.entcore.audience.services.impl.EventBusAudienceAccessFilter;
import org.entcore.common.user.UserInfos;
import org.entcore.common.user.UserUtils;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class AudienceController extends BaseController {

  private final AudienceAccessFilter audienceAccessFilter;

  private final ReactionService reactionService;

  private final ViewService viewService;

  public AudienceController(final Vertx vertx, final JsonObject config, final ReactionService reactionService,
                            final ViewService viewService) {
    this.audienceAccessFilter = new EventBusAudienceAccessFilter(vertx);
    this.reactionService = reactionService;
    this.viewService = viewService;
  }


  @Get("/reactions/:module/:resourceType")
  @SecuredAction(value = "", type = ActionType.AUTHENTICATED)
  public void getReactionsSummary(final HttpServerRequest request) {
    String module = request.getParam("module");
    String resourceType = request.getParam("resourceType");
    final Set<String> resourceIds = RequestUtils.getParamAsSet("resourceIds", request);

    verify(module, resourceType, resourceIds, request)
        .onSuccess(user -> reactionService.getReactionsSummary(module, resourceType, resourceIds, user)
                .onSuccess(reactionsSummary -> Renders.renderJson(request, JsonObject.mapFrom(reactionsSummary))));
  }

  @Get("/reactions/:module/:resourceType/:resourceId")
  @SecuredAction(value = "", type = ActionType.AUTHENTICATED)
  public void getReactionDetails(final HttpServerRequest request) {
    final String resourceId = request.getParam("resourceId");
    verify(request.getParam("module"), request.getParam("resourceType"), Collections.singleton(resourceId), request)
        .onSuccess(user -> reactionService.getReactionDetails(resourceId, request, user));
  }

  @Post("/reactions/:module/:resourceType")
  @SecuredAction(value = "", type = ActionType.AUTHENTICATED)
  public void createReaction(final HttpServerRequest request) {
    doUpsertReaction(request);
  }

  @Put("/reactions/:module/:resourceType")
  @SecuredAction(value = "", type = ActionType.AUTHENTICATED)
  public void updateReaction(final HttpServerRequest request) {
    doUpsertReaction(request);
  }

  @Delete("/reactions/:module/:resourceType/:resourceId")
  @SecuredAction(value = "", type = ActionType.AUTHENTICATED)
  public void deleteReaction(final HttpServerRequest request) {
    final String resourceId = request.getParam("resourceId");
    verify(request.getParam("module"), request.getParam("resourceType"), Collections.singleton(resourceId), request)
        .onSuccess(user -> reactionService.deleteReaction(resourceId, request, user)
            .onSuccess(e -> Renders.ok(request))
            .onFailure(th -> {
              Renders.log.error("Error while deleting reaction for user and resource", th);
            }));
  }

  @Post("/views/:module/:resourceType/:resourceId")
  @SecuredAction(value = "", type = ActionType.AUTHENTICATED)
  public void registerView(final HttpServerRequest request) {
    final String resourceId = request.getParam("resourceId");
    final String module = request.getParam("module");
    final String resourceType = request.getParam("resourceType");
    verify(module, resourceType, Collections.singleton(resourceId), request)
        .onSuccess(user -> viewService.registerView(module, resourceType, resourceId, user)
            .onSuccess(e -> Renders.ok(request))
            .onFailure(th -> {
              Renders.log.error("Error while registering resource view for resource " + module + "@" + resourceType + "@" + resourceId, th);
            }));
  }

  @Get("/views/count/:module/:resourceType")
  @SecuredAction(value = "", type = ActionType.AUTHENTICATED)
  public void getResourcesViewCounts(final HttpServerRequest request) {
    final Set<String> resourceIds = RequestUtils.getParamAsSet("resourceIds", request);
    final String module = request.getParam("module");
    final String resourceType = request.getParam("resourceType");
    verify(module, resourceType, resourceIds, request)
    .onSuccess(user -> viewService.getViewCounts(module, resourceType, resourceIds, user)
        .onSuccess(e -> Renders.render(request, e))
        .onFailure(th -> {
          Renders.log.error("Error while getting views of resource " + module + "@" + resourceType + "@" + resourceIds, th);
        }));
  }

  @Get("/views/details/:module/:resourceType/:resourceId")
  @SecuredAction(value = "", type = ActionType.AUTHENTICATED)
  public void getResourcesViewDetails(final HttpServerRequest request) {
    final String resourceId = request.getParam("resourceId");
    final String module = request.getParam("module");
    final String resourceType = request.getParam("resourceType");
    verify(module, resourceType, Collections.singleton(resourceId), request)
    .onSuccess(user -> viewService.getViewDetails(module, resourceType, resourceId, user)
        .onSuccess(e -> Renders.render(request, e))
        .onFailure(th -> {
          Renders.log.error("Error while getting views details of resource " + module + "@" + resourceType + "@" + resourceId, th);
        }));
  }


  public void doUpsertReaction(final HttpServerRequest request) {
    RequestUtils.bodyToJson(request, jsonBody -> {
      final String module = request.getParam("module");
      final String resourceType = request.getParam("resourceType");
      final String resourceId = jsonBody.getString("resourceId", "");
      final ReactionType reactionType = ReactionType.valueOf(jsonBody.getString("reactionType", ""));
      final Set<String> resourceIds = new HashSet<>();
      resourceIds.add(resourceId);
      verify(module, resourceType, resourceIds, request)
          .onSuccess(user -> reactionService.upsertReaction(module, resourceType, resourceId, user, reactionType)
                  .onSuccess(upsertedReaction -> Renders.ok(request))
                  .onFailure(th -> Renders.renderError(request)));
    });
  }

  /**
   * @param module Name of the application which owns the resources to check
   * @param resourceType Type of the resources to check
   * @param resourceIds Ids of the resources whose access by the user should be
   *                    checked
   * @param request     Incoming request to verify
   * @return Succeeds iff the requesting user if this user can access the
   *         resources, <b>does not complete otherwise</b> but send a response to
   *         the user.
   */
  private Future<UserInfos> verify(final String module, final String resourceType, final Set<String> resourceIds, final HttpServerRequest request) {
    final Promise<UserInfos> promise = Promise.promise();
    UserUtils.getAuthenticatedUserInfos(eb, request).onSuccess(user -> audienceAccessFilter.canAccess(module, resourceType, user, resourceIds)
            .onSuccess(canAccess -> {
              if (canAccess) {
                promise.complete(user);
              } else {
                promise.fail("user.cannot.access");
                Renders.forbidden(request);
              }})
            .onFailure(th -> {
              Renders.log.error("Error while fetching the user credentials to access the resources", th);
              promise.fail(th);
              Renders.renderError(request);
            }));
    return promise.future();
  }
}
