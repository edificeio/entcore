package edu.one.core.infra;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.vertx.java.core.Handler;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.http.HttpServerRequest;
import org.vertx.java.core.http.RouteMatcher;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.logging.Logger;
import org.vertx.java.platform.Container;

import edu.one.core.infra.http.Binding;
import edu.one.core.infra.http.HttpMethod;
import edu.one.core.infra.request.filter.SecurityHandler;

public abstract class AbstractService {

	private static final MethodHandles.Lookup lookup = MethodHandles.publicLookup();
	protected final Vertx vertx;
	protected final Container container;
	private final RouteMatcher rm;
	private final Map<String, Set<Binding>> uriBinding;
	private final Map<String, String> securedActions;
	protected final Logger log;

	public AbstractService(Vertx vertx, Container container, RouteMatcher rm, Map<String, String> securedActions) {
		this.vertx = vertx;
		this.container = container;
		this.rm = rm;
		this.uriBinding = new HashMap<>();
		this.log = container.logger();
		this.securedActions = securedActions;
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

	private Handler<HttpServerRequest> executeSecure(String method) {
		try {
			final MethodHandle mh = lookup.bind(this, method,
					MethodType.methodType(void.class, HttpServerRequest.class));
			return new SecurityHandler() {

				@Override
				public void filter(HttpServerRequest request) {
					try {
						mh.invokeExact(request);
					} catch (Throwable e) {
						request.response().setStatusCode(500).end();
					}
				}
			};
		} catch (NoSuchMethodException | IllegalAccessException e) {

			return new SecurityHandler() {

				@Override
				public void filter(HttpServerRequest request) {
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

	private Handler<HttpServerRequest> bindHandler(String method) {
		if (method == null || method.trim().isEmpty()) {
			throw new NullPointerException();
		}
		if (securedActions.containsKey(method)) {
			return executeSecure(method);
		}
		return execute(method);
	}

	public Map<String, Set<Binding>> getUriBinding() {
		return this.uriBinding;
	}

	public Map<String, Set<Binding>> getSecuredUriBinding() {
		Map<String, Set<Binding>> bindings = new HashMap<>();
		for (Entry<String, Set<Binding>> e : this.uriBinding.entrySet()) {
			if (securedActions.containsKey(e.getKey())) {
				bindings.put(e.getKey(), e.getValue());
			}
		}
		return bindings;
	}

	public Set<Binding> securedUriBinding() {
		Set<Binding> bindings = new HashSet<>();
		for (Entry<String, Set<Binding>> e : this.uriBinding.entrySet()) {
			if (securedActions.containsKey(e.getKey())) {
				bindings.addAll(e.getValue());
			}
		}
		return bindings;
	}

	public AbstractService get(String pattern, String method) {
		addPattern(pattern, HttpMethod.GET, method);
		rm.get(pattern, bindHandler(method));
		return this;
	}

	public AbstractService put(String pattern, String method) {
		addPattern(pattern, HttpMethod.PUT, method);
		rm.put(pattern, bindHandler(method));
		return this;
	}

	public AbstractService post(String pattern, String method) {
		addPattern(pattern, HttpMethod.POST, method);
		rm.post(pattern, bindHandler(method));
		return this;
	}

	public AbstractService delete(String pattern, String method) {
		addPattern(pattern, HttpMethod.DELETE, method);
		rm.delete(pattern, bindHandler(method));
		return this;
	}

	public AbstractService getWithRegEx(String regex, String method) {
		addRegEx(regex, HttpMethod.GET, method);
		rm.getWithRegEx(regex, bindHandler(method));
		return this;
	}

	public AbstractService putWithRegEx(String regex, String method) {
		addRegEx(regex, HttpMethod.PUT, method);
		rm.putWithRegEx(regex, bindHandler(method));
		return this;
	}

	public AbstractService postWithRegEx(String regex, String method) {
		addRegEx(regex, HttpMethod.POST, method);
		rm.postWithRegEx(regex, bindHandler(method));
		return this;
	}

	public AbstractService deleteWithRegEx(String regex, String method) {
		addRegEx(regex, HttpMethod.DELETE, method);
		rm.deleteWithRegEx(regex, bindHandler(method));
		return this;
	}

	private void addPattern(String input, HttpMethod httpMethod, String serviceMethod) {
		Matcher m = Pattern.compile(":([A-Za-z][A-Za-z0-9_]*)").matcher(input);
		StringBuffer sb = new StringBuffer();
		Set<String> groups = new HashSet<>();
		while (m.find()) {
			String group = m.group().substring(1);
			if (groups.contains(group)) {
				throw new IllegalArgumentException("Cannot use identifier " + group + " more than once in pattern string");
			}
			m.appendReplacement(sb, "(?<$1>[^\\/]+)");
			groups.add(group);
		}
		m.appendTail(sb);
		String regex = sb.toString();
		Set<Binding> bindings = uriBinding.get(serviceMethod);
		if (bindings == null) {
			bindings = new HashSet<>();
			uriBinding.put(serviceMethod, bindings);
		}
		bindings.add(new Binding(httpMethod, Pattern.compile(regex), serviceMethod));
	}

	private void addRegEx(String input, HttpMethod httpMethod, String serviceMethod) {
		Set<Binding> bindings = uriBinding.get(serviceMethod);
		if (bindings == null) {
			bindings = new HashSet<>();
			uriBinding.put(serviceMethod, bindings);
		}
		bindings.add(new Binding(httpMethod, Pattern.compile(input), serviceMethod));
	}

}
