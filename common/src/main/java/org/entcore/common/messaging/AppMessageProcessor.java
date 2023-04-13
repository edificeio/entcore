package org.entcore.common.messaging;

import io.vertx.core.Future;
import org.entcore.common.messaging.to.ClientMessage;

import java.util.function.Function;

/**
 * Service that will process messages one by one and notify the caller when it is done.
 * @param <T> The type of message handled by instances of this class.
 * <pre>{@code
 * public class MyProcessor implements AppMessageProcessor<MessageType> {
 *    @Override
 *    public Class<MessageType> getHandledMessageClass() {
 * 		return MessageType.class;
 *    }
 *
 *    @Override
 *    public Future apply(final MessageType o) {
 * 		final Promise<MyReport> promise = Promise.promise();
 * 	    // Do async stuf
 * 	    // ....
 * 	    // and somewhere call promise.complete or promise.fail
 * 	    return promise.future();
 *    }
 * }
 *
 * ...
 *
 * this.messagingClient.startListening(new MyProcessor() {});
 * }</pre>
 */
public interface AppMessageProcessor<T extends ClientMessage> extends Function<T, Future> {
    /**
     * @return Class of the messages handled by this processor
     */
    Class<T> getHandledMessageClass();
}
