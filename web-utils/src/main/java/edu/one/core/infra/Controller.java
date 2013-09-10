package edu.one.core.infra;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.vertx.java.core.Handler;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.eventbus.EventBus;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.http.HttpServerRequest;
import org.vertx.java.core.http.RouteMatcher;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.platform.Container;

import edu.one.core.infra.http.Binding;
import edu.one.core.infra.http.HttpMethod;
import edu.one.core.infra.http.Renders;
import edu.one.core.infra.request.filter.SecurityHandler;
import edu.one.core.infra.security.ActionType;
import edu.one.core.infra.security.SecuredAction;
import edu.one.core.infra.security.UserUtils;

public abstract class Controller extends Renders {

	private static final MethodHandles.Lookup lookup = MethodHandles.publicLookup();
	protected final Vertx vertx;
	protected final Container container;
	private final RouteMatcher rm;
	private final Map<String, Set<Binding>> uriBinding;
	private final Map<String, SecuredAction> securedActions;
	protected final EventBus eb;


	public Controller(Vertx vertx, Container container, RouteMatcher rm,
			Map<String, SecuredAction> securedActions) {
		super(container);
		this.vertx = vertx;
		this.container = container;
		this.rm = rm;
		this.uriBinding = new HashMap<>();
		this.securedActions = securedActions;
		this.eb = Server.getEventBus(vertx);
	}

	private Handler<HttpServerRequest> execute(final String method) {
		try {
			final MethodHandle mh = lookup.bind(this, method,
					MethodType.methodType(void.class, HttpServerRequest.class));
			return new Handler<HttpServerRequest>() {

				@Override
				public void handle(HttpServerRequest request) {
					try {
						mh.invokeExact(request);
					} catch (Throwable e) {
						log.error("Error invoking secured method : " + method, e);
						request.response().setStatusCode(500).end();
					}
				}
			};
		} catch (NoSuchMethodException | IllegalAccessException e) {

			return new Handler<HttpServerRequest>() {

				@Override
				public void handle(HttpServerRequest request) {
					log.error("Error mapping method : " + method, e);
					request.response().setStatusCode(404).end();
				}
			};
		}
	}

	private Handler<HttpServerRequest> executeSecure(final String method) {
		try {
			final MethodHandle mh = lookup.bind(this, method,
					MethodType.methodType(void.class, HttpServerRequest.class));
			return new SecurityHandler() {

				@Override
				public void filter(HttpServerRequest request) {
					try {
						mh.invokeExact(request);
					} catch (Throwable e) {
						log.error("Error invoking secured method : " + method, e);
						request.response().setStatusCode(500).end();
					}
				}
			};
		} catch (NoSuchMethodException | IllegalAccessException e) {

			return new SecurityHandler() {

				@Override
				public void filter(HttpServerRequest request) {
					log.error("Error mapping secured method : " + method, e);
					request.response().setStatusCode(404).end();
				}
			};
		}
	}

	public void registerMethod(String address, String method)
			throws NoSuchMethodException, IllegalAccessException {
		final MethodHandle mh = lookup.bind(this, method,
				MethodType.methodType(void.class, Message.class));
		Server.getEventBus(vertx).registerHandler(address, new Handler<Message<JsonObject>>() {

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
		if (securedActions.containsKey(this.getClass().getName() + "|" + method)) {
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

	public Controller get(String pattern, String method) {
		pattern = addPathPrefix(pattern);
		addPattern(pattern, HttpMethod.GET, method);
		rm.get(pattern, bindHandler(method));
		return this;
	}

	public Controller put(String pattern, String method) {
		pattern = addPathPrefix(pattern);
		addPattern(pattern, HttpMethod.PUT, method);
		rm.put(pattern, bindHandler(method));
		return this;
	}

	public Controller post(String pattern, String method) {
		pattern = addPathPrefix(pattern);
		addPattern(pattern, HttpMethod.POST, method);
		rm.post(pattern, bindHandler(method));
		return this;
	}

	public Controller delete(String pattern, String method) {
		pattern = addPathPrefix(pattern);
		addPattern(pattern, HttpMethod.DELETE, method);
		rm.delete(pattern, bindHandler(method));
		return this;
	}

	public Controller getWithRegEx(String regex, String method) {
		regex = addPathPrefix(regex).replaceAll("\\/", "\\/");
		addRegEx(regex, HttpMethod.GET, method);
		rm.getWithRegEx(regex, bindHandler(method));
		return this;
	}

	public Controller putWithRegEx(String regex, String method) {
		regex = addPathPrefix(regex).replaceAll("\\/", "\\/");
		addRegEx(regex, HttpMethod.PUT, method);
		rm.putWithRegEx(regex, bindHandler(method));
		return this;
	}

	public Controller postWithRegEx(String regex, String method) {
		regex = addPathPrefix(regex).replaceAll("\\/", "\\/");
		addRegEx(regex, HttpMethod.POST, method);
		rm.postWithRegEx(regex, bindHandler(method));
		return this;
	}

	public Controller deleteWithRegEx(String regex, String method) {
		regex = addPathPrefix(regex).replaceAll("\\/", "\\/");
		addRegEx(regex, HttpMethod.DELETE, method);
		rm.deleteWithRegEx(regex, bindHandler(method));
		return this;
	}

	private void addPattern(String input, HttpMethod httpMethod, String method) {
		String serviceMethod = this.getClass().getName() + "|" + method;
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
		bindings.add(new Binding(httpMethod, Pattern.compile(regex), serviceMethod, actionType(serviceMethod)));
	}

	private void addRegEx(String input, HttpMethod httpMethod, String method) {
		String serviceMethod = this.getClass().getName() + "|" + method;
		Set<Binding> bindings = uriBinding.get(serviceMethod);
		if (bindings == null) {
			bindings = new HashSet<>();
			uriBinding.put(serviceMethod, bindings);
		}
		bindings.add(new Binding(httpMethod, Pattern.compile(input), serviceMethod, actionType(serviceMethod)));
	}

	private ActionType actionType(String serviceMethod) {
		try {
			return ActionType.valueOf(securedActions.get(serviceMethod).getType());
		} catch (IllegalArgumentException | NullPointerException e) {
			return ActionType.UNSECURED;
		}
	}

	private String addPathPrefix(String pattern) {
		if (pattern == null || pattern.trim().isEmpty()) {
			return pathPrefix;
		}
		if (!pattern.trim().startsWith("/")) {
			return pathPrefix + "/" + pattern.trim();
		}
		return pathPrefix + pattern.trim();
	}

	protected void shareResource(final HttpServerRequest request,
			final List<String> checked) {
		final String id = request.params().get("id");
		if (id != null && !id.trim().isEmpty()) {
			Map<String, JsonObject> resourceActions = new HashMap<>();
			for (SecuredAction action: securedActions.values()) {
				if (ActionType.RESOURCE.name().equals(action.getType())) {
					JsonObject a = resourceActions.get(action.getDisplayName());
					if (a == null) {
						a = new JsonObject()
						.putString("name", action.getName())
						.putString("displayName", I18n.getInstance().translate(action.getDisplayName(), request.headers().get("Accept-Language")))
						.putString("type", action.getType());
						resourceActions.put(action.getDisplayName(), a);
					} else {
						a.putString("name", a.getString("name") + "," + action.getName());
					}
				}
			}
			final JsonArray actions = new JsonArray(resourceActions.values().toArray());
			UserUtils.findVisibleUsers(eb, request, new Handler<JsonArray>() {
				@Override
				public void handle(JsonArray visibleUsers) {
					JsonArray users = new JsonArray();
					for(Object u : visibleUsers) {
						JsonObject user = (JsonObject) u;
						JsonArray userChoices = new JsonArray();
						for (Object a: actions) {
							JsonObject action = (JsonObject) a;
							String value = action.getString("name").replaceAll("\\.", "-");
							List<String> list = new ArrayList<>();
							for (String s:  Arrays.asList(value.split(","))) {
								list.add(s + "_" + user.getString("id"));
							}
							value += "_" + user.getString("id");
							JsonObject c = new JsonObject()
								.putString("value", value);
							if (checked != null && checked.containsAll(list)) {
								c.putString("checked", "checked");
							} else {
								c.putString("checked", "");
							}
							userChoices.add(c);
						}
						users.add(new JsonObject()
							.putObject("user", user)
							.putArray("choices", userChoices));
					}
					JsonObject share = new JsonObject()
						.putString("postUri", request.path())
						.putString("resourceId", id)
						.putArray("actions", actions)
						.putArray("users", users);
					renderView(request, share, "/view/shareResource.html");
				}
			});
		} else {
			renderView(request, null, "/view/resourceNotFound.html");
		}
	}

	private void renderView(HttpServerRequest request,
			JsonObject share, String resourceName) {
		InputStream in = this.getClass().getResourceAsStream(resourceName);
		Reader r = new InputStreamReader(in);
		renderView(request, share, resourceName, r);
	}

}
