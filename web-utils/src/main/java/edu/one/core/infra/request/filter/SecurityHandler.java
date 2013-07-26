package edu.one.core.infra.request.filter;

import java.util.ArrayList;
import java.util.List;

import org.vertx.java.core.Handler;
import org.vertx.java.core.http.HttpServerRequest;

import edu.one.core.infra.security.SecureHttpServerRequest;
/*
 * Implement a Security Handler with a pre-configurate filters chain 
 */
public abstract class SecurityHandler implements Handler<HttpServerRequest> {

	static protected List<Filter> chain = new ArrayList<>();
	static {
		chain.add(new UserAuthFilter());
		chain.add(new AppAuthFilter());
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private Handler<Boolean> chainToHandler(final HttpServerRequest request) {
		final Handler [] handlers = new Handler[chain.size()];
		handlers[chain.size() - 1] = new Handler<Boolean>() {

			@Override
			public void handle(Boolean access) {
				if (Boolean.TRUE.equals(access)) {
					filter(request);
				} else {
					chain.get(chain.size() - 1).deny(request);
				}
			}
		};
		for (int i = chain.size() - 2; i >= 0; i--) {
			final int idx = i;

			handlers[i] = new Handler<Boolean>() {

				@Override
				public void handle(Boolean access) {
					if (Boolean.TRUE.equals(access)) {
						chain.get(idx + 1).canAccess(request, handlers[idx + 1]);
					} else {
						chain.get(idx).deny(request);
					}
				}
			};
		}

		return handlers[0];
	}

	@Override
	public void handle(HttpServerRequest request) {
		if (chain != null && !chain.isEmpty()) {
			SecureHttpServerRequest sr = new SecureHttpServerRequest(request);
			chain.get(0).canAccess(sr, chainToHandler(sr));
		} else {
			filter(request);
		}
	}

	public static void addFilter(Filter filter) {
		chain.add(filter);
	}

	public abstract void filter(HttpServerRequest request);
}
