package org.entcore.common.messaging;

import io.vertx.core.Future;
import org.entcore.common.messaging.to.ClientMessage;

import java.util.Collections;
import java.util.List;

/**
 * A service that allows asynchronous communication between different parts of the ENT on a dedicated canal.
 *
 * Following is a typical use of the client :
 *
 * <pre>{@code
 * // Initialize
 *
 * final IMessagingClient messagingClient;
 * final JsonObject myConfiguration = serviceConfig.getJsonObject("myConfiguration");
 * if (myConfiguration != null && myConfiguration.getBoolean("enabled", false)) {
 * 	MessagingClientFactoryProvider.init(vertx);
 * 	messagingClient = MessaginClientFactoryProvider.getFactory(myConfiguration.getJsonObject("messaging")).create();
 * } else {
 * 	messagingClient = IMessagingClient.noop;
 * }
 *
 * ...
 *
 * // To send messages
 *
 * final Message message = new Message(System.currentTimeMillis(), userId);
messagingClient.pushMessages(fileMetadata)
.onSuccess(e -> log.debug("Successfully sent message"))
.onFailure(th -> log.warn("Could not send message", th));
 *
 * ...
 *
 * // To treat messages
 *
 * this.messagingClient.startListening(new AppMessageProcessor<MessageType>() {
 *    @Override
 *    public Class<MessageType> getHandledMessageClass() {
 * 		return MessageType.class;
 *    }
 *
 *    @Override
 *    public Future apply(final MessageType o) {
 * 		return Future.failedFuture("not.implemented");
 *    }
 * });
 *
 *
 * }</pre>
 */
public interface IMessagingClient {

    /**
     *  Implementation that does nothing.
     *  Should be used when no configuration was found for the client to avoid NPEs and nullity checks.
     * **/
    static final IMessagingClient noop = new NoopMessagingClient();

    /**
     * Send messages and return when the messages were sent or failed to be sent (so we do not wait for an ACK from an
     * eventual lsitener).
     * @param messages Messages to be sent to the destination
     * @return A list of identifier for each sent message (respecting {@code messages} order)
     */
    Future<List<String>> pushMessages(final Object... messages);

    /**
     * @return the name of the address to send and receive messages
     */
    String getAddress();

    /**
     * Start listening to messages coming at the specified address
     * @param handler Things to do when a message arrives.
     * @return A future which completes when the client is ready to process message.
     * @param <T> The type of the messages to treat
     */
    <T extends ClientMessage> Future<Void> startListening(final AppMessageProcessor<T> handler);

    /**
     * Stops the client from listening to new messages but do not stop the processing of already read messages.
     * @return A future which completes when the client has stopped processing new messages
     */
    Future<Void> stopListening();

    boolean canListen();

    boolean isListening();

    /**
     * Dummy implementation that does nothing.
     */
    public static class NoopMessagingClient implements IMessagingClient {

        private NoopMessagingClient() {}
        @Override
        public Future<List<String>> pushMessages(final Object... messages) {
            return Future.succeededFuture(Collections.emptyList());
        }

        @Override
        public String getAddress() {
            return null;
        }

        @Override
        public <T extends ClientMessage> Future<Void> startListening(final AppMessageProcessor<T> handler) {
            return null;
        }

        @Override
        public Future<Void> stopListening() {
            return Future.succeededFuture();
        }

        @Override
        public boolean canListen() {
            return false;
        }

        @Override
        public boolean isListening() {
            return false;
        }
    }
}
