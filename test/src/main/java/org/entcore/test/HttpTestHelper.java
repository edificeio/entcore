package org.entcore.test;

import com.fasterxml.jackson.databind.ObjectMapper;
import fr.wseduc.webutils.http.Binding;
import fr.wseduc.webutils.http.HttpMethod;
import fr.wseduc.webutils.security.ActionType;
import fr.wseduc.webutils.security.SecureHttpServerRequest;
import fr.wseduc.webutils.validation.JsonSchemaValidator;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.MultiMap;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.entcore.common.http.request.JsonHttpServerRequest;
import org.entcore.common.http.response.JsonHttpResponse;
import org.entcore.common.user.UserInfos;
import org.entcore.common.validation.StringValidation;

import java.net.URI;
import java.util.*;
import java.util.regex.Pattern;

public class HttpTestHelper {
    final TestHelper helper;

    public HttpTestHelper(TestHelper h) {
        helper = h;
    }

    public TestHttpServerRequest put(String url) {
        return put(url, new JsonObject());
    }

    public TestHttpServerRequest put(String url, JsonObject params) {
        return request(HttpMethod.PUT, url, params);
    }

    public TestHttpServerRequest put(String url, JsonObject params, JsonObject body) {
        return request(HttpMethod.PUT, url, params, body);
    }

    public TestHttpServerRequest post(String url) {
        return post(url, new JsonObject());
    }

    public TestHttpServerRequest post(String url, JsonObject params) {
        return request(HttpMethod.POST, url, params);
    }

    public TestHttpServerRequest post(String url, JsonObject params, JsonObject body) {
            return request(HttpMethod.POST, url, params, body);
    }

    public TestHttpServerRequest get(String url) {
        return get(url, new JsonObject());
    }

    public TestHttpServerRequest get(String url, JsonObject params) {
        return request(HttpMethod.GET, url, params);
    }

    protected JsonObject urlToHttpServerRequestParams(String url) {
        final URI uri = URI.create(url);
        return new JsonObject()
            .put("ssl", "https".equals(uri.getScheme()))
            .put("scheme", uri.getScheme())
            .put("path", uri.getPath())
            .put("query", uri.getQuery())
            .put("uri", uri.toString());
    }

    public TestHttpServerRequest request(HttpMethod method, String url, JsonObject params) {
        JsonObject json = urlToHttpServerRequestParams(url)
            .put("method", method.name())
            .put("params", params);
        return new TestHttpServerRequest(json);
    }

    public TestHttpServerRequest request(final HttpMethod method, final String url, final JsonObject params, final JsonObject body) {
        JsonObject json = urlToHttpServerRequestParams(url)
            .put("method", method.name())
            .put("params", params);
        return new TestHttpServerRequest(json, body);
    }

    public HttpTestHelper mockJsonValidator(){
        JsonSchemaValidator.getInstance().setEventBus(helper.vertx.eventBus());
        JsonSchemaValidator.getInstance().setAddress("json.schema.validator");
        helper.vertx.eventBus().consumer("json.schema.validator").handler(e->{
            e.reply(new JsonObject().put("status", "ok"));
        });
        return this;
    }

    public Binding binding(HttpMethod httpMethod, Class<?> clazz, String method) {
        String name = clazz.getName() + "|" + method;
        return new Binding(httpMethod, Pattern.compile(""), name, ActionType.RESOURCE);
    }

    public JsonObject randomPayloadFromSchema(String jsonSchema) {
        final JsonObject schema = helper.file().jsonFromResource(jsonSchema);
        final JsonObject properties = schema.getJsonObject("properties");
        final JsonArray required = schema.getJsonArray("required");
        final JsonObject out = new JsonObject();
        final Random random = new Random();

        for (Object o : required) {
            if (!(o instanceof String))
                continue;
            final String attr = (String) o;
            final String type = properties.getJsonObject(attr).getString("type");
            switch (type) {
                case "string":
                    out.put(attr, StringValidation.generateRandomCode(8));
                    break;
                case "boolean":
                    out.put(attr, false);
                    break;
                case "integer":
                    out.put(attr, random.nextInt());
                    break;
            }
        }
        return out;
    }

    public UserInfos sessionUser() {

        return helper.directory().generateUser("a1234");
    }

    public static class TestHttpServerRequest extends JsonHttpServerRequest {
        MultiMap headers = MultiMap.caseInsensitiveMultiMap();
        JsonObject body = new JsonObject();

        public TestHttpServerRequest(JsonObject object) {
            super(object, new TestHttpServerResponse());
        }

        public TestHttpServerRequest(JsonObject object, JsonObject body) {
            super(object, new TestHttpServerResponse());
            this.body = body;
        }

        @Override
        public TestHttpServerResponse response() {
            return (TestHttpServerResponse)super.response();
        }

        @Override
        public MultiMap headers() {
            return headers;
        }

        @Override
        public TestHttpServerRequest bodyHandler(Handler<Buffer> bufferHandler) {
            bufferHandler.handle(Buffer.buffer(body.encode()));
            return this;
        }
        
        public HttpServerRequest withSession(final UserInfos user) throws Exception {
            final ObjectMapper mapper = new ObjectMapper();
            final SecureHttpServerRequest req = new SecureHttpServerRequest(this);
            req.setSession(new JsonObject(mapper.writeValueAsString(user)));
            return req;
        }

        public HttpServerRequest withOAuthToken() {
            final SecureHttpServerRequest req = new SecureHttpServerRequest(this);
            req.headers().add("Authorization", "Bearer " + UUID.randomUUID().toString());
            return req;
        }
    }

    public static class TestHttpServerResponse extends JsonHttpResponse {
        Handler<Object> bodyHandler = e -> {};

        public TestHttpServerResponse bodyHandler(Handler<Object> bufferHandler) {
            bodyHandler = bufferHandler;
            return this;
        }

        public TestHttpServerResponse jsonHandler(Handler<JsonObject> bufferHandler) {
            bodyHandler = e->{
                bufferHandler.handle(new JsonObject(e.toString()));
            };
            return this;
        }


        public TestHttpServerResponse endJsonHandler(Handler<JsonObject> bufferHandler) {
            final JsonObject res = new JsonObject();
            jsonHandler(json ->{
                res.mergeIn(json);
            });
            endHandler(e->{
                bufferHandler.handle(res);
            });
            return this;
        }

        public TestHttpServerResponse jsonArrayHandler(Handler<JsonArray> bufferHandler) {
            bodyHandler = e->{
                bufferHandler.handle(new JsonArray(e.toString()));
            };
            return this;
        }


        public TestHttpServerResponse endJsonArrayHandler(Handler<JsonArray> bufferHandler) {
            final JsonArray res = new JsonArray();
            jsonArrayHandler(json ->{
                res.addAll(json);
            });
            endHandler(e->{
                bufferHandler.handle(res);
            });
            return this;
        }

        @Override
        public Future<Void> write(Buffer buffer) {
            bodyHandler.handle(buffer);
            return Future.succeededFuture();
        }

        @Override
        public Future<Void> write(String s, String s2) {
            bodyHandler.handle(s);
            return Future.succeededFuture();
        }

        @Override
        public Future<Void> write(String s) {
            bodyHandler.handle(s);
            return Future.succeededFuture();
        }

        @Override
        public Future<Void> end(String s) {
            bodyHandler.handle(s);
            return super.end(s);
        }

        @Override
        public Future<Void> end(String s, String s2) {
            bodyHandler.handle(s);
            return super.end(s, s2);
        }

        @Override
        public Future<Void> end(Buffer buffer) {
            bodyHandler.handle(buffer);
            return super.end(buffer);
        }

    }
}