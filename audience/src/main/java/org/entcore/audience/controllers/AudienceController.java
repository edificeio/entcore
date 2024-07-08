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
import io.vertx.core.eventbus.MessageConsumer;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import org.entcore.audience.reaction.service.ReactionService;
import org.entcore.audience.services.AudienceAccessFilter;
import org.entcore.audience.services.AudienceService;
import org.entcore.audience.view.service.ViewService;
import org.entcore.audience.services.impl.EventBusAudienceAccessFilter;
import org.entcore.common.audience.AudienceHelper;
import org.entcore.common.audience.to.NotifyResourceDeletionMessage;
import org.entcore.common.audience.to.NotifyResourceDeletionResponseMessage;
import org.entcore.common.http.filter.Trace;
import org.entcore.common.user.UserInfos;
import org.entcore.common.user.UserUtils;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class AudienceController extends BaseController {
  public static final String CREATE_REACTION_ACTION = "AUDIENCE_CREATE_REACTION";
  public static final String REGISTER_VIEW_ACTION = "AUDIENCE_REGISTER_VIEW";
  private final AudienceAccessFilter audienceAccessFilter;

  private final ReactionService reactionService;

  private final ViewService viewService;

  private final AudienceService audienceService;

  private final MessageConsumer<Object> resourceDeletionListener;

  private final Set<String> validReactionTypes;

  public AudienceController(final Vertx vertx, final JsonObject config, final ReactionService reactionService,
                            final ViewService viewService, final AudienceService audienceService, Set<String> validReactionTypes) {
    this.audienceAccessFilter = new EventBusAudienceAccessFilter(vertx);
    this.reactionService = reactionService;
    this.viewService = viewService;
    this.audienceService = audienceService;
    resourceDeletionListener = listenForResourceDeletionNotification(vertx);
    this.validReactionTypes = validReactionTypes;
  }


  @Get("/reactions/:module/:resourceType")
  @SecuredAction(value = "", type = ActionType.AUTHENTICATED)
  public void getReactionsSummary(final HttpServerRequest request) {
    final String module = request.getParam("module");
    final String resourceType = request.getParam("resourceType");
    final Set<String> resourceIds = RequestUtils.getParamAsSet("resourceIds", request);

    verify(module, resourceType, resourceIds, request)
        .onSuccess(user -> reactionService.getReactionsSummary(module, resourceType, resourceIds, user)
                .onSuccess(reactionsSummary -> Renders.render(request, reactionsSummary))
                .onFailure(th -> {
                  Renders.log.error("Error while getting reactions summary on resource " + module + "@" + resourceType + "@" + resourceIds, th);
                  Renders.renderError(request);
                }));
  }

  @Get("/reactions/:module/:resourceType/:resourceId")
  @SecuredAction(value = "", type = ActionType.AUTHENTICATED)
  public void getReactionDetails(final HttpServerRequest request) {
    final String module = request.getParam("module");
    final String resourceType = request.getParam("resourceType");
    final String resourceId = request.getParam("resourceId");
    final int page = Integer.parseInt(request.getParam("page"));
    final int size = Integer.parseInt(request.getParam("size"));
    verify(module, resourceType, Collections.singleton(resourceId), request)
        .onSuccess(user -> reactionService.getReactionDetails(module, resourceType, resourceId, page, size)
                .onSuccess(reactionDetailsResponse -> Renders.render(request, reactionDetailsResponse))
                .onFailure(th -> {
                  Renders.log.error("Error while getting reaction details on resource " + module + "@" + resourceType + "@" + resourceId, th);
                  Renders.renderError(request);
                }));
  }

  @Trace(value = CREATE_REACTION_ACTION, retentionDays = 5)
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
    final String module = request.getParam("module");
    final String resourceType = request.getParam("resourceType");
    final String resourceId = request.getParam("resourceId");
    verify(module, resourceType, Collections.singleton(resourceId), request)
        .onSuccess(user -> reactionService.deleteReaction(module, resourceType, resourceId, user)
            .onSuccess(e -> Renders.render(request, e))
            .onFailure(th -> {
              Renders.log.error("Error while deleting reaction for user and resource " + module + "@" + resourceType + "@" + resourceId, th);
              Renders.renderError(request);
            }));
  }
  @Trace(value = REGISTER_VIEW_ACTION, retentionDays = 5, body = false)
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
              Renders.renderError(request);
            }));
  }

  @Get("/views/count/:module/:resourceType")
  @SecuredAction(value = "", type = ActionType.AUTHENTICATED)
  public void getResourcesViewCounts(final HttpServerRequest request) {
    final Set<String> resourceIds = RequestUtils.getParamAsSet("resourceIds", request);
    final String module = request.getParam("module");
    final String resourceType = request.getParam("resourceType");
    verify(module, resourceType, resourceIds, request)
    .onSuccess(user -> viewService.getViewCounters(module, resourceType, resourceIds)
        .onSuccess(e -> Renders.render(request, e))
        .onFailure(th -> {
          Renders.log.error("Error while getting views of resource " + module + "@" + resourceType + "@" + resourceIds, th);
          Renders.renderError(request);
        }));
  }

  @Get("/views/details/:module/:resourceType/:resourceId")
  @SecuredAction(value = "", type = ActionType.AUTHENTICATED)
  public void getResourcesViewDetails(final HttpServerRequest request) {
    final String resourceId = request.getParam("resourceId");
    final String module = request.getParam("module");
    final String resourceType = request.getParam("resourceType");
    verify(module, resourceType, Collections.singleton(resourceId), request)
    .onSuccess(user -> viewService.getViewDetails(module, resourceType, resourceId)
        .onSuccess(e -> Renders.render(request, e))
        .onFailure(th -> {
          Renders.log.error("Error while getting views details of resource " + module + "@" + resourceType + "@" + resourceId, th);
          Renders.renderError(request);
        }));
  }


  public void doUpsertReaction(final HttpServerRequest request) {
    RequestUtils.bodyToJson(request, jsonBody -> {
      final String module = request.getParam("module");
      final String resourceType = request.getParam("resourceType");
      final String resourceId = jsonBody.getString("resourceId", "");
      final String reactionType = jsonBody.getString("reactionType", "");
      if (validReactionTypes.contains(reactionType)) {
          final Set<String> resourceIds = new HashSet<>();
          resourceIds.add(resourceId);
          verify(module, resourceType, resourceIds, request)
                  .onSuccess(user ->
                          reactionService.upsertReaction(module, resourceType, resourceId, user, reactionType)
                                  .onSuccess(upsertedReaction -> Renders.ok(request))
                                  .onFailure(th -> {
                                      Renders.log.error("Error while upserting reaction on resource " + module + "@" + resourceType + "@" + resourceId, th);
                                      Renders.renderError(request);
                                  }));
      } else {
          Renders.badRequest(request, "Specified reaction type is not valid : " + reactionType);
      }
    });
  }

  public MessageConsumer<Object> listenForResourceDeletionNotification(Vertx vertx) {
      final MessageConsumer<Object> consumer = vertx.eventBus().consumer(AudienceHelper.AUDIENCE_ADDRESS);
      consumer.handler(message -> {
          Object body = message.body();
          if (body == null) {
              log.warn("Received a null message from " + message.replyAddress());
              message.reply(Json.encode(new NotifyResourceDeletionResponseMessage("message.mandatory")));
          } else {
              try {
                  final NotifyResourceDeletionMessage resourceDeletionMessage = Json.decodeValue((String) body, NotifyResourceDeletionMessage.class);
                  audienceService.purgeDeletedResources(resourceDeletionMessage.getModule(), resourceDeletionMessage.getResourceType(), resourceDeletionMessage.getResourceIds())
                          .onSuccess(event -> message.reply(Json.encode(new NotifyResourceDeletionResponseMessage(true))))
                          .onFailure(th -> {
                              log.warn("An error occurred while purging audience on deleted resources " + body, th);
                              message.reply(Json.encode(new NotifyResourceDeletionResponseMessage("purge.error")));
                          });
              } catch (Exception e) {
                  log.warn("Received a message from " + message.replyAddress() + "that could not be converted to " + NotifyResourceDeletionMessage.class.getCanonicalName() + " : " + body);
                  message.reply(Json.encode(new NotifyResourceDeletionResponseMessage("message.bad.format")));
              }
          }
      }).exceptionHandler(th -> log.warn("An error occurred in the purge handler during deleted resources notification", th));
      return consumer;
  }

  public void stopResourceDeletionListener() {
      if (resourceDeletionListener != null) {
          resourceDeletionListener.unregister();
      }
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
