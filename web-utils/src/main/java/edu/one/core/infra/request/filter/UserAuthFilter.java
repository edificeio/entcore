package edu.one.core.infra.request.filter;

import edu.one.core.infra.request.CookieUtils;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import org.vertx.java.core.Handler;
import org.vertx.java.core.http.HttpServerRequest;

public class UserAuthFilter implements Filter {

	@Override
	public void canAccess(HttpServerRequest request, Handler<Boolean> handler) {
		String oneSeesionId = CookieUtils.get("oneSessionId", request);
		if (oneSeesionId == null) {
			handler.handle(false);
		} else {
			handler.handle(true);
		}
	}

	@Override
	public void deny(HttpServerRequest request) {
		String callBack = "";
		try {
			callBack = "http://" + URLEncoder.encode(request.headers().get("Host") + request.uri(), "UTF-8");
		} catch (UnsupportedEncodingException ex) {
		}

		String location = "http://localhost:8009/login?callback=" + callBack;
		request.response().setStatusCode(301);
		request.response().putHeader("Location", location);
		request.response().end();
	}

}
