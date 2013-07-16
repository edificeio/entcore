package edu.one.core.infra;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.HashMap;
import java.util.Map;

import org.vertx.java.core.Handler;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.http.HttpServerRequest;
import org.vertx.java.core.http.RouteMatcher;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.platform.Container;

public abstract class AbstractService {

	private static final MethodHandles.Lookup lookup = MethodHandles.publicLookup();
	protected final Vertx vertx;
	protected final Container container;
	private final RouteMatcher rm;
	private final Map<String, String> uriBinding;

	public AbstractService(Vertx vertx, Container container, RouteMatcher rm) {
		this.vertx = vertx;
		this.container = container;
		this.rm = rm;
		this.uriBinding = new HashMap<>();
	}

	private Handler<HttpServerRequest> execute(String method) {
		try {
			final MethodHandle mh = lookup.bind(this, method,
					MethodType.methodType(void.class, HttpServerRequest.class));
			return new Handler<HttpServerRequest>() {

				@Override
				public void handle(HttpServerRequest request) {
					try {
						mh.invokeExact(request);
					} catch (Throwable e) {
						request.response().setStatusCode(500).end();
					}
				}
			};
		} catch (NoSuchMethodException | IllegalAccessException e) {

			return new Handler<HttpServerRequest>() {

				@Override
				public void handle(HttpServerRequest request) {
					request.response().setStatusCode(404).end();
				}
			};
		}
	}

	public void registerMethod(String address, String method)
			throws NoSuchMethodException, IllegalAccessException {
		final MethodHandle mh = lookup.bind(this, method,
				MethodType.methodType(void.class, Message.class));
		vertx.eventBus().registerHandler(address, new Handler<Message<JsonObject>>() {

			@Override
			public void handle(Message<JsonObject> message) {
				try {
					mh.invokeExact(message);
				} catch (Throwable e) {
					container.logger().error(e.getMessage(), e);
					JsonObject json = new JsonObject().putString("status", "error")
							.putString("message", e.getMessage());
					message.reply(json);
				}
			}
		});
	}

	public Map<String, String> getUriBinding() {
		return this.uriBinding;
	}

	public AbstractService get(String pattern, String method) {
		uriBinding.put("GET " + pattern, method);
		rm.get(pattern, execute(method));
		return this;
	}

	public AbstractService put(String pattern, String method) {
		uriBinding.put("PUT " + pattern, method);
		rm.put(pattern, execute(method));
		return this;
	}

	public AbstractService post(String pattern, String method) {
		uriBinding.put("POST " + pattern, method);
		rm.post(pattern, execute(method));
		return this;
	}

	public AbstractService delete(String pattern, String method) {
		uriBinding.put("DELETE " + pattern, method);
		rm.delete(pattern, execute(method));
		return this;
	}

	public AbstractService getWithRegEx(String regex, String method) {
		uriBinding.put("GET " + regex, method);
		rm.getWithRegEx(regex, execute(method));
		return this;
	}

	public AbstractService putWithRegEx(String regex, String method) {
		uriBinding.put("PUT " + regex, method);
		rm.putWithRegEx(regex, execute(method));
		return this;
	}

	public AbstractService postWithRegEx(String regex, String method) {
		uriBinding.put("POST " + regex, method);
		rm.postWithRegEx(regex, execute(method));
		return this;
	}

	public AbstractService deleteWithRegEx(String regex, String method) {
		uriBinding.put("DELETE " + regex, method);
		rm.deleteWithRegEx(regex, execute(method));
		return this;
	}

}
