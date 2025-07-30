package org.entcore.common.http.filter;

import com.google.common.collect.Maps;
import fr.wseduc.webutils.http.Binding;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonObject;
import org.entcore.common.user.UserInfos;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Map;

public class PreAuthorizeFilter implements ResourcesProvider{

    private final Map<String, ExpressionContext> filtersMapping = Maps.newHashMap();

    private static final Logger LOGGER = LoggerFactory.getLogger(PreAuthorizeFilter.class);

    public PreAuthorizeFilter() {
        loadConfiguration();
    }

    protected void loadConfiguration() {
        InputStream is = BaseResourceProvider.class.getClassLoader().getResourceAsStream(
                "PreAuthorize.json");
        if (is != null) {
            try (BufferedReader r = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
                String line;
                while ((line = r.readLine()) != null) {
                    JsonObject filter = new JsonObject(line);
                    String method = filter.getString("method");
                    String f = filter.getString("expression");
                    String fullQualifedClass = filter.getString("clazz");
                    Class<?> evaluator = Class.forName(fullQualifedClass);
                    if (f != null && method != null &&
                            !f.trim().isEmpty() && !method.trim().isEmpty()) {
                            filtersMapping.put(method, new ExpressionContext(f, evaluator));
                    }
                }
            } catch (IOException e) {
                LOGGER.error("Unable to load filters in " + this.getClass().getName(), e);
            } catch (ClassNotFoundException e) {
                LOGGER.error("Unable to load class ", e);
            }
        } else {
            LOGGER.warn("Not found resource filter file.");
        }
    }


    @Override
    public void authorize(HttpServerRequest resourceRequest, Binding binding, UserInfos user, Handler<Boolean> handler) {

    }

    private static class ExpressionContext {
        public final String expression;
        public final Class<?> evaluator;

        ExpressionContext(String expression, Class<?> evaluator) {
            this.expression = expression;
            this.evaluator = evaluator;
        }
    }
}
