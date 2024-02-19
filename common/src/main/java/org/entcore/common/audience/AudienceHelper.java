package org.entcore.common.audience;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.MessageConsumer;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.Json;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.entcore.common.audience.to.*;
import org.entcore.common.user.UserInfos;

/**
 * Helper class to interact with audience service.
 */
public class AudienceHelper {

    private final Logger log = LoggerFactory.getLogger(AudienceHelper.class);

    public AudienceHelper(final Vertx vertx) {
    }

    public Future<ViewIncrementationResponse> incrementView(
            final ViewIncrementationRequest request,
            final String resourceType,
            final HttpServerRequest httpRequest,
            final UserInfos user) {
        throw new UnsupportedOperationException("not.yet.implemented");
        /*return restClient.post("/view/counters/{platformId}/{module}/{resourceType}", request, params,
                getHeadersToForward(httpRequest),
                ViewIncrementationResponse.class);*/
    }

    public Future<ViewsResponse> getViews(final UserInfos user,
            final HttpServerRequest httpRequest, final String... resources) {
        throw new UnsupportedOperationException("not.yet.implemented");
        /*return restClient.get("/view/counters/{platformId}/{module}/{resourceType}?resourceIds={resourceIds}", params,
                getHeadersToForward(httpRequest), ViewsResponse.class);*/
    }

    public Future<ViewDetailsResponse> getViewDetails(final String resourceId,
            final HttpServerRequest httpRequest, final UserInfos user) {
        throw new UnsupportedOperationException("not.yet.implemented");
        /*return restClient.get("/view/details/{platformId}/{module}/{resourceType}/{resourceId}", params,
                getHeadersToForward(httpRequest), ViewDetailsResponse.class);*/
    }


    public void onDeletedResource(final String resourceId,
            final HttpServerRequest httpRequest, final UserInfos user) {
        throw new UnsupportedOperationException("not.yet.implemented");
        /*return restClient.delete("/reactions/{platformId}/{module}/{resourceType}/{resourceId}", params,
                getHeadersToForward(httpRequest), ReactionDetailsResponse.class);*/
    }

    public void onDeletedUser(final String userId,
            final HttpServerRequest httpRequest) {
        throw new UnsupportedOperationException("not.yet.implemented");
        /*return restClient.delete("/reactions/{platformId}/{module}/{resourceType}/{resourceId}", params,
                getHeadersToForward(httpRequest), ReactionDetailsResponse.class);*/
    }

    public Future<Void> deleteReactionForUserAndResource(String resourceId, HttpServerRequest request, UserInfos user) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'deleteReactionForUserAndResource'");
    }
    public static String getCheckRightsBusAddress(final String appName, final String resourceType) {
        return "audience.check.right." + appName + "." + resourceType;
    }

    /**
     * Start a listener that will listen for messages to check a user's ability to react or view a set of resources.
     * @param appName Name of the app of the resources that can be checked
     * @param resourceType Type of the resources  that can be checked
     * @param vertx Vertx instance
     * @param checker The function to be called when a new message that matches the appName and resourceType arrives
     * @return The listener (that can be unregistered when the verticle is undeployed)
     */
    public MessageConsumer<Object> listenForRightsCheck(final String appName, final String resourceType,
                                     final Vertx vertx, final AudienceRightChecker checker) {
        final EventBus eb = vertx.eventBus();
        final MessageConsumer<Object> consumer = eb.consumer(getCheckRightsBusAddress(appName, resourceType));
        consumer.handler(message -> {
            Object body = message.body();
            if(body == null) {
                log.warn("Received a null message from " + message.replyAddress());
                message.reply(new AudienceCheckRightResponseMessage("message.mandatory"));
            } else {
                try {
                    final AudienceCheckRightRequestMessage checkRightMessage = Json.decodeValue((String) body, AudienceCheckRightRequestMessage.class);
                    checker.apply(checkRightMessage)
                        .onSuccess(access -> message.reply(new AudienceCheckRightResponseMessage(access)))
                        .onFailure(th -> {
                            log.warn("An error occurred while checking " + AudienceCheckRightRequestMessage.class.getCanonicalName() + " for message " + body, th);
                            message.reply(new AudienceCheckRightResponseMessage("check.error"));
                        });
                } catch (Exception e) {
                    log.warn("Received a message from " + message.replyAddress() + " that could not be converted to " + AudienceCheckRightRequestMessage.class.getCanonicalName() + " : " + body);
                    message.reply(new AudienceCheckRightResponseMessage("message.bad.format"));
                }
            }
        })
        .exceptionHandler(th -> log.warn("An error occurred in the rights checker handler for appName = " + appName + ", resourceType = " + resourceType, th));
        return consumer;
    }
}
