package edu.one.core.infra.request.filter;

import java.util.ArrayList;
import java.util.List;
import org.vertx.java.core.Handler;
import org.vertx.java.core.http.HttpServerRequest;

/*
 * Implement a Security Handler with a pre-configurate filter chain (User / Auth / App)
 *
 */
public abstract class FilterChainHandler implements Handler<HttpServerRequest> {

	private List<Filter> chain = new ArrayList<>();

	// Tmp : Externalise Authentication Filter
	{
		chain.add(new Filter() {
			@Override
			public boolean test(HttpServerRequest request) {
				String cookies = request.headers().get("Cookie");
				String oneID;
				System.out.println("cookies: " + cookies);
				if (request.params().get("oneID") != null) {
					return true;
				}
				if (cookies == null) {
					return false;
				}
				String[] ar = cookies.split("=");
				System.out.println("ar: " + ar);
				if ("oneID".equals(ar[0])) {
					oneID = ar[1];
					System.out.println("oneID : " + oneID);
					return true;
				}
				return false;
			}
		});
	}

	@Override
	public void handle(HttpServerRequest request) {
		for (Filter filter : chain) {
			if (!filter.test(request)) {
				request.response.statusCode = 301;
				request.response.putHeader("Location", "http://localhost:8009/login");
				request.response.end();
				return;
			}
		}
		request.response.putHeader("Set-Cookie", "oneID=1234");
		filterAndHandle(request);
	}

	abstract public void filterAndHandle(HttpServerRequest request);
}
