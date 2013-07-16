package edu.one.core.infra.request.filter;

import org.vertx.java.core.http.HttpServerRequest;

public interface Filter {

	boolean canAccess (HttpServerRequest request);
	void deny (HttpServerRequest request);

}
