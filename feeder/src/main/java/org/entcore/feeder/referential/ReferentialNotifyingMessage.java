package org.entcore.feeder.referential;

import io.vertx.codegen.annotations.Nullable;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.MultiMap;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

/**
 * Manual action message that notifies referential of the touched entities once the action succeeded (reply with
 * status "ok"). A failed notification is only logged (action name, no data): the next resync catches up.
 */
public class ReferentialNotifyingMessage implements Message<JsonObject> {

	private static final Logger log = LoggerFactory.getLogger(ReferentialNotifyingMessage.class);

	private final Message<JsonObject> inner;
	private final ReferentialPublisher publisher;
	private final String action;
	/** Copy of the message at its arrival: the action may alter it (the validation removes "childrenIds"). */
	private final JsonObject request;

	public ReferentialNotifyingMessage(Message<JsonObject> inner, ReferentialPublisher publisher, String action) {
		this.inner = inner;
		this.publisher = publisher;
		this.action = action;
		this.request = inner.body() == null ? new JsonObject() : inner.body().copy();
	}

	private void notifyReferential(Object reply) {
		if (!(reply instanceof JsonObject) || !"ok".equals(((JsonObject) reply).getString("status"))) {
			return;
		}
		try {
			final EntityChangedDTO changes = ReferentialChanges.of(action, request, reply);
			if (!changes.touchesNothing()) {
				publisher.entityChanged(changes).onFailure(e -> log.warn("transmission_failed action=" + action));
			}
		} catch (RuntimeException e) {
			log.warn("transmission_failed action=" + action + " " + e.getClass().getSimpleName());
		}
	}

	@Override
	public void reply(Object message) {
		inner.reply(message);
		notifyReferential(message);
	}

	@Override
	public void reply(Object message, DeliveryOptions options) {
		inner.reply(message, options);
		notifyReferential(message);
	}

	@Override
	public <R> Future<Message<R>> replyAndRequest(@Nullable Object message, DeliveryOptions options) {
		notifyReferential(message);
		return inner.replyAndRequest(message, options);
	}

	@Override
	public <R> Future<Message<R>> replyAndRequest(@Nullable Object message) {
		notifyReferential(message);
		return inner.replyAndRequest(message);
	}

	@Override
	public <R> void replyAndRequest(Object message, DeliveryOptions options, Handler<AsyncResult<Message<R>>> handler) {
		notifyReferential(message);
		inner.replyAndRequest(message, options, handler);
	}

	@Override
	public <R> void replyAndRequest(Object message, Handler<AsyncResult<Message<R>>> handler) {
		notifyReferential(message);
		inner.replyAndRequest(message, handler);
	}

	@Override
	public void fail(int failureCode, String message) {
		inner.fail(failureCode, message);
	}

	@Override
	public boolean isSend() {
		return inner.isSend();
	}

	@Override
	public String replyAddress() {
		return inner.replyAddress();
	}

	@Override
	public JsonObject body() {
		return inner.body();
	}

	@Override
	public MultiMap headers() {
		return inner.headers();
	}

	@Override
	public String address() {
		return inner.address();
	}
}
