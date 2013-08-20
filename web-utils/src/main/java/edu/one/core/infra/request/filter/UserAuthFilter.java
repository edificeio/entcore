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
		String location = "";
		try {
			callBack = "http://" + URLEncoder.encode(request.headers().get("Host") + request.uri(), "UTF-8");
			location = "http://" + URLEncoder.encode(request.headers().get("Host").split(":")[0], "UTF-8")
					+ ":8009/login?callback=" + callBack;
		} catch (UnsupportedEncodingException ex) {
			ex.printStackTrace();
		}
		request.response().setStatusCode(302);
		request.response().putHeader("Location", location);
		request.response().end();
	}

}
