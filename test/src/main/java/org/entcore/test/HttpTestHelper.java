package org.entcore.test;

import fr.wseduc.webutils.http.Binding;
import fr.wseduc.webutils.http.HttpMethod;
import fr.wseduc.webutils.security.ActionType;
import io.vertx.core.MultiMap;
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

    public HttpServerRequest get(String url) {
        return get(url, new JsonObject());
    }

    public HttpServerRequest get(String url, JsonObject params) {
        return request(HttpMethod.GET, url, params);
    }

    public HttpServerRequest request(HttpMethod method, String url, JsonObject params) {
        final URI uri = URI.create(url);
        JsonObject json = new JsonObject();
        json.put("method", method.name());
        json.put("ssl", "https".equals(uri.getScheme()));
        json.put("scheme", uri.getScheme());
        json.put("path", uri.getPath());
        json.put("query", uri.getQuery());
        json.put("uri", uri.toString());
        json.put("params", params);
        return new TestHttpServerRequest(json);
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

    static class TestHttpServerRequest extends JsonHttpServerRequest {
        MultiMap headers = MultiMap.caseInsensitiveMultiMap();

        public TestHttpServerRequest(JsonObject object) {
            super(object, new TestHttpServerResponse());
        }

        @Override
        public MultiMap headers() {
            return headers;
        }

    }

    static class TestHttpServerResponse extends JsonHttpResponse {

    }
}