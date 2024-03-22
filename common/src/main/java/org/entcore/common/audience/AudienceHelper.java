package org.entcore.common.audience;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.MessageConsumer;
import io.vertx.core.json.Json;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.entcore.common.audience.to.AudienceCheckRightRequestMessage;
import org.entcore.common.audience.to.AudienceCheckRightResponseMessage;
import org.entcore.common.audience.to.NotifyResourceDeletionMessage;
import org.entcore.common.audience.to.NotifyResourceDeletionResponseMessage;

import java.util.Set;

/**
 * Helper class to interact with audience service.
 */
public class AudienceHelper {

    public static final String AUDIENCE_ADDRESS = "entcore.audience";

    private static final Logger log = LoggerFactory.getLogger(AudienceHelper.class);

    private final Vertx vertx;

    public AudienceHelper(final Vertx vertx) {
        this.vertx = vertx;
    }

    public static String getCheckRightsBusAddress(final String module, final String resourceType) {
        return "audience.check.right." + module + "." + resourceType;
    }

    /**
     * Method to notify audience module that a resources have been deleted
     * @param module the module of the resources
     * @param resourceType the resource type of the resources
     * @param resourceIds the resources id
     * @return a complete future if the triggered audience action is successful
     */
    public Future<Void> notifyResourcesDeletion(final String module, final String resourceType, final Set<String> resourceIds) {
        Promise<Void> promise = Promise.promise();
        if (!resourceIds.isEmpty()) {
            vertx.eventBus().request(AUDIENCE_ADDRESS, Json.encode(new NotifyResourceDeletionMessage(module, resourceType, resourceIds)), messageAsyncResult -> {
                if (messageAsyncResult.succeeded()) {
                    final NotifyResourceDeletionResponseMessage responseMessage = Json.decodeValue((String) messageAsyncResult.result().body(), NotifyResourceDeletionResponseMessage.class);
                    if (responseMessage.isSuccess()) {
                        promise.complete();
                    } else {
                        promise.fail(responseMessage.getErrorMessage());
                    }
                } else {
                    promise.fail(messageAsyncResult.cause());
                }
            });
        } else {
            promise.complete();
        }
        return promise.future();
    }

    /**
     * Start a listener that will listen for messages to check a user's ability to react or view a set of resources.
     * @param module Name of the app of the resources that can be checked
     * @param resourceType Type of the resources  that can be checked
     * @param checker The function to be called when a new message that matches the module and resourceType arrives
     * @return The listener (that can be unregistered when the verticle is undeployed)
     */
    public MessageConsumer<Object> listenForRightsCheck(final String module, final String resourceType, final AudienceRightChecker checker) {
        final EventBus eb = vertx.eventBus();
        final MessageConsumer<Object> consumer = eb.consumer(getCheckRightsBusAddress(module, resourceType));
        consumer.handler(message -> {
            Object body = message.body();
            if(body == null) {
                log.warn("Received a null message from " + message.replyAddress());
                message.reply(new AudienceCheckRightResponseMessage("message.mandatory"));
            } else {
                try {
                    final AudienceCheckRightRequestMessage checkRightMessage = Json.decodeValue((String) body, AudienceCheckRightRequestMessage.class);
                    checker.apply(checkRightMessage)
                        .onSuccess(access -> message.reply(Json.encode(new AudienceCheckRightResponseMessage(access))))
                        .onFailure(th -> {
                            log.warn("An error occurred while checking " + AudienceCheckRightRequestMessage.class.getCanonicalName() + " for message " + body, th);
                            message.reply(Json.encode(new AudienceCheckRightResponseMessage("check.error")));
                        });
                } catch (Exception e) {
                    log.warn("Received a message from " + message.replyAddress() + " that could not be converted to " + AudienceCheckRightRequestMessage.class.getCanonicalName() + " : " + body);
                    message.reply(Json.encode(new AudienceCheckRightResponseMessage("message.bad.format")));
                }
            }
        })
        .exceptionHandler(th -> log.warn("An error occurred in the rights checker handler for module = " + module + ", resourceType = " + resourceType, th));
        return consumer;
    }
}
