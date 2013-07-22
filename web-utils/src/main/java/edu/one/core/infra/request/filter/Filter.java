package edu.one.core.infra.request.filter;

import org.vertx.java.core.Handler;
import org.vertx.java.core.http.HttpServerRequest;

public interface Filter {

	void canAccess (HttpServerRequest request, Handler<Boolean> handler);
	void deny (HttpServerRequest request);

}
