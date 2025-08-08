package org.entcore.common.http.filter.expression;

import io.vertx.core.Handler;
import io.vertx.core.http.HttpServerRequest;
import org.entcore.common.user.UserInfos;

public class ExpressionEvaluatorContext {

    public ExpressionEvaluatorContext(HttpServerRequest request, String[] methodArgument, Handler<Boolean> handler, UserInfos usersInfo) {
        this.request = request;
        this.methodArgument = methodArgument;
        this.handler = handler;
        this.usersInfo = usersInfo;
    }

    public HttpServerRequest request;
    public String[] methodArgument;
    public Handler<Boolean> handler;
    public UserInfos usersInfo;

}
