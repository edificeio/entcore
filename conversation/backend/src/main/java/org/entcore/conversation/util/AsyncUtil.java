package org.entcore.conversation.util;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;

/**
 * Asynchronous helpers with no dependency on any service of the module.
 */
public final class AsyncUtil {

	private AsyncUtil() {
	}

	/**
	 * Yields the event loop for the given delay, so chained treatments do not run back to back.
	 * @param vertx the vertx instance holding the timer
	 * @param millis how long to yield for, a value of zero or less completing immediately
	 * @return a {@link Future} completed once the delay has elapsed
	 */
	public static Future<Void> pause(final Vertx vertx, final long millis) {
		if (millis <= 0) {
			return Future.succeededFuture();
		}
		final Promise<Void> promise = Promise.promise();
		vertx.setTimer(millis, timerId -> promise.complete());
		return promise.future();
	}
}
