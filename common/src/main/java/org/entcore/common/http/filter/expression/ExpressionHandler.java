package org.entcore.common.http.filter.expression;

import fr.wseduc.webutils.http.Binding;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpServerRequest;
import org.entcore.common.http.filter.PreAuthorizeFilter;
import org.entcore.common.user.UserInfos;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ExpressionHandler  {

    private final Pattern p = Pattern.compile("(\\w*)\\('([^']+)'\\)");
    private static final Logger LOGGER = LoggerFactory.getLogger(ExpressionHandler.class);

    public void authorize(HttpServerRequest resourceRequest, Binding binding,
                          UserInfos user, Handler<Boolean> handler, PreAuthorizeFilter.ExpressionContext expressionContext) {

        Matcher m = p.matcher(expressionContext.expression);
        if (!m.matches()) {
            LOGGER.error(" Security expression {} cant be parsed", expressionContext.expression);
            handler.handle(false);
            return;
        }
        String function = m.group(1);
        String argument = m.group(2);
        try {
            Object instance = expressionContext.evaluator.getDeclaredConstructor().newInstance();
            Method method = expressionContext.evaluator.getMethod(function, ExpressionEvaluatorContext.class);
            method.invoke(instance, new ExpressionEvaluatorContext(resourceRequest, new String[] { argument }, handler, user));
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            LOGGER.error(" Security method can't be invoked {} on {}", function, expressionContext.evaluator);
            handler.handle(false);
        }

    }
}
