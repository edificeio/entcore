package edu.one.core.infra.request.filter;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.vertx.java.core.Handler;
import org.vertx.java.core.http.HttpServerRequest;
/*
 * Implement a Security Handler with a pre-configurate filters chain 
 */
public abstract class SecurityHandler implements Handler<HttpServerRequest> {

	static protected List<Filter> chain = new ArrayList<>();
	{
		chain.add(new UserAuthFilter());
		chain.add(new AppAuthFilter());
	}

	@Override
	public void handle(HttpServerRequest request) {
		for (Iterator<Filter> it = chain.iterator(); it.hasNext();) {
			Filter f = it.next();
			if (!f.canAccess(request)) {
				f.deny(request);
				break;
			}
			if (!it.hasNext()){
				filter(request);
			}
		}

	}

	public abstract void filter(HttpServerRequest request);
}
