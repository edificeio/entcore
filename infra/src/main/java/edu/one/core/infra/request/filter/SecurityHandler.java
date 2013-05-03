package edu.one.core.infra.request.filter;

import java.util.ArrayList;
import java.util.List;
import org.vertx.java.core.Handler;
import org.vertx.java.core.http.HttpServerRequest;

/*
 * Implement a Security Handler with a pre-configurate filter chain (User / Auth / App
 */
public abstract class SecurityHandler implements Handler<HttpServerRequest> {

	static protected List<Filter> chain = new ArrayList<>();
	{
		chain.add(new UserAuthFilter());
		chain.add(new AppAuthFilter());
	}

	public void filter(HttpServerRequest request) {
		boolean canAccess = true;
		for (Filter f : chain) {
			canAccess &= f.verify(request);
		}
		if (!canAccess) {
			chain.get(0).deny(request);
		}
		handle(request);
	}
}
