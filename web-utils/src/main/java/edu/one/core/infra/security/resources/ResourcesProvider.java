package edu.one.core.infra.security.resources;

import org.vertx.java.core.Handler;
import org.vertx.java.core.http.HttpServerRequest;

public interface ResourcesProvider {

	void authorize(HttpServerRequest resourceRequest, UserInfos user, Handler<Boolean> handler);

}
