package org.entcore.common.utils;

import fr.wseduc.webutils.validation.JsonSchemaValidator;
import io.vertx.core.Future;
import io.vertx.core.MultiMap;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class HttpUtils {
    private static Logger log = LoggerFactory.getLogger(HttpUtils.class);
    private static final JsonSchemaValidator validator = JsonSchemaValidator.getInstance();

    public static Future<JsonObject> getAndCheckQueryParams(final String pathPrefix, final String schema, final MultiMap queryParams) {
        return getAndCheckQueryParams(pathPrefix, schema, queryParams, true);
    }

    public static Future<JsonObject> getAndCheckQueryParams(final String pathPrefix, final String schema, final MultiMap queryParams, final boolean tryParse) {
        final JsonObject json = new JsonObject();
        for (final String name : queryParams.names()) {
            final List<String> values = queryParams.getAll(name);
            final List<Object> parsedValues = values.stream().map(e->{
                if(tryParse){
                    final Optional<Boolean> optBool = StringUtils.parseBoolean(e);
                    if(optBool.isPresent()){
                        return optBool.get();
                    }
                    final Optional<Long> optLong = StringUtils.parseLong(e);
                    if(optLong.isPresent()){
                        return optLong.get();
                    }
                    final Optional<Double> optDouble = StringUtils.parseDouble(e);
                    if(optDouble.isPresent()){
                        return optDouble.get();
                    }
                    return e;
                }else{
                    return e;
                }
            }).collect(Collectors.toList());
            if (parsedValues.size() == 1) {
                json.put(name, parsedValues.iterator().next());
            } else {
                json.put(name, new JsonArray(parsedValues));
            }
        }
        //validate
        final Promise<JsonObject> promise = Promise.promise();
        validator.validate(pathPrefix + schema, json, res -> {
            if (res.succeeded()) {
                final JsonObject body = res.result().body();
                if ("ok".equals(body.getString("status"))) {
                    promise.complete(json);
                } else {
                    promise.fail(body.getString("message"));
                }
            } else {
                log.error("Validate async error.", res.cause());
                promise.fail(res.cause());
            }
        });
        return promise.future();
    }
}
