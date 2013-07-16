package edu.one.core.infra.request.filter;

import org.vertx.java.core.http.HttpServerRequest;

public class AppAuthFilter implements Filter {

	@Override
	public boolean canAccess(HttpServerRequest request) {
		return true;
	}

	@Override
	public void deny(HttpServerRequest request) {
		throw new UnsupportedOperationException("Not supported yet.");
	}

}
