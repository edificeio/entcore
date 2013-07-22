package edu.one.core.infra.request.filter;

import org.vertx.java.core.Handler;
import org.vertx.java.core.http.HttpServerRequest;

public class AppAuthFilter implements Filter {

	@Override
	public void canAccess(HttpServerRequest request, Handler<Boolean> handler) {
		handler.handle(true);
	}

	@Override
	public void deny(HttpServerRequest request) {
		throw new UnsupportedOperationException("Not supported yet.");
	}

}
