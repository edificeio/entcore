package org.entcore.common.trace;

import fr.wseduc.mongodb.MongoDb;
import fr.wseduc.webutils.http.Binding;
import fr.wseduc.webutils.http.Renders;
import fr.wseduc.webutils.request.filter.Filter;
import fr.wseduc.webutils.security.SecureHttpServerRequest;
import fr.wseduc.webutils.security.XSSUtils;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.core.Promise;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.DecodeException;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.entcore.common.http.filter.Trace;
import org.entcore.common.user.UserInfos;
import org.entcore.common.user.UserUtils;
import org.entcore.common.utils.Config;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.*;
import java.util.regex.Matcher;

public class TraceFilter implements Filter {

    private static final String RETENTION_DAYS = "retention-days";
    private static final int DEFAULT_RETENTION_DAYS = 365;
    private static final Logger log = LoggerFactory.getLogger(TraceFilter.class);
    private final EventBus eb;
    private final Set<Binding> bindings;
    private Set<Binding> tracedBinding;
    HashMap<String, JsonObject> actions = new HashMap<>();
    private static final List<String> bodyMethods = Arrays.asList("POST", "PUT");

    private static final String COLLECTION = "traces";

    public TraceFilter(EventBus eb, Set<Binding> bindings) {
        this.eb = eb;
        this.bindings = bindings;
    }

    private void loadTracedMethods() {
        tracedBinding = new HashSet<>();
        InputStream is = TraceFilter.class.getClassLoader().getResourceAsStream(Trace.class.getSimpleName() + ".json");
        if (is != null) {
            BufferedReader r = null;
            try {
                r = new BufferedReader(new InputStreamReader(is, "UTF-8"));
                String line;
                while ((line = r.readLine()) != null) {
                    final JsonObject trace = new JsonObject(line);
                    for (Binding binding : bindings) {
                        if (binding != null && trace.getString("method", "").equals(binding.getServiceMethod())) {
                            tracedBinding.add(binding);
                            JsonObject action = new JsonObject()
                                    .put("value", trace.getString("value"))
                                    .put("body", trace.getBoolean("body"))
                                    .put(RETENTION_DAYS, trace.getInteger(RETENTION_DAYS, DEFAULT_RETENTION_DAYS));
                            actions.put(binding.getServiceMethod(), action);
                            break;
                        }
                    }
                }
            } catch (IOException | DecodeException e) {
                log.error("Unable to load traced methods", e);
            } finally {
                if (r != null) {
                    try {
                        r.close();
                    } catch (IOException e) {
                        log.error("Close input stream error", e);
                    }
                }
            }
        }
    }

    @Override
    public void canAccess(HttpServerRequest request, Handler<Boolean> handler) {
        handler.handle(true);
        JsonObject entry = MongoDb.now();
        if (tracedBinding == null) {
            loadTracedMethods();
        }

        if (request instanceof SecureHttpServerRequest && trace(request)) {
            final long epoch = System.currentTimeMillis();
            request.response().endHandler(event -> {
                List<Future> futures = new ArrayList<>();
                Promise<UserInfos> userFuture = Promise.promise();
	            Promise<Object> bodyFuture = Promise.promise();
                futures.add(userFuture.future());
                final JsonObject action = actions.get(getRequestServiceMethod(request));
                if (bodyMethods.contains(request.method().name()) && action.getBoolean("body")) {
                    futures.add(bodyFuture.future());
                    getBody(request, bodyFuture);
                }
                CompositeFuture.all(futures).onComplete(futureEvent -> {
                    if (futureEvent.failed()) {
                        log.error("[TraceFilter] Failed to create trace : " + request.uri(), futureEvent.cause());
                        return;
                    }
                    UserInfos user = userFuture.future().result();

                    JsonObject trace = new JsonObject()
                            .put("application", Config.getInstance().getConfig().getString("app-name"))
                            .put("user", getUserObject(user))
                            .put("request", getActionObject(request))
                            .put("entry", entry)
                            .put("date", epoch)
                            .put(RETENTION_DAYS, action.getInteger(RETENTION_DAYS, DEFAULT_RETENTION_DAYS))
                            .put("response", MongoDb.now())
                            .put("action", action.getString("value"))
                            .put("status", request.response().getStatusCode());

                    if (bodyMethods.contains(request.method().name()) && action.getBoolean("body")) {
                        trace.put("resource", bodyFuture.future().result());
                    }

                    MongoDb.getInstance().insert(COLLECTION, trace);
                });

                getUser(request, userFuture);
            });
        }
    }

    private JsonObject getUserObject(UserInfos user) {
        return new JsonObject()
                .put("login", user.getLogin())
                .put("id", user.getUserId());
    }

    private JsonObject getActionObject (HttpServerRequest request) {
        JsonObject parameters = new JsonObject();
        Iterator<Map.Entry<String, String>> iterator = request.params().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, String> param = iterator.next();
            parameters.put(param.getKey(), param.getValue());
        }
        final JsonObject event = new JsonObject()
                .put("method", request.method().name())
                .put("uri", request.uri())
                .put("params", parameters);
        final String ua = request.headers().get("User-Agent");
        if (ua != null) {
            event.put("ua", ua);
        }
        final String ip = Renders.getIp(request);
        if (ip != null) {
            event.put("ip", ip);
        }
        final String xapp = request.headers().get("X-APP");
        if (xapp != null) {
            event.put("xapp", xapp);
        }
        return event;
    }

    private void getUser(HttpServerRequest request, Promise<UserInfos> future) {
        UserUtils.getUserInfos(this.eb, request, user -> {
            if (user == null) {
                future.fail("Null user");
            } else {
                future.complete(user);
            }
        });
    }

    private void getBody(HttpServerRequest request, Promise<Object> future) {
        request.bodyHandler(bodyEvt -> {
            String body = XSSUtils.stripXSS(bodyEvt.toString("UTF-8"));
            if (body == null || "".equals(body.trim())){
                future.complete(null);
            } else {
                Object parsedBody  = body.startsWith("{") ? new JsonObject(body) : new JsonArray(body);
                future.complete(parsedBody);
            }
        });
    }

    private String getRequestServiceMethod(HttpServerRequest request) {
        for (Binding binding : tracedBinding) {
            if (!request.method().name().equals(binding.getMethod().name())) {
                continue;
            }
            Matcher m = binding.getUriPattern().matcher(request.path());
            if (m.matches()) {
                return binding.getServiceMethod();
            }
        }

        return "";
    }

    private boolean trace(HttpServerRequest request) {
        if (!tracedBinding.isEmpty()) {
            for (Binding binding : tracedBinding) {
                if (!request.method().name().equals(binding.getMethod().name())) {
                    continue;
                }
                Matcher m = binding.getUriPattern().matcher(request.path());
                if (m.matches()) return true;
            }
        }
        return false;
    }

    @Override
    public void deny(HttpServerRequest httpServerRequest) {}
}
