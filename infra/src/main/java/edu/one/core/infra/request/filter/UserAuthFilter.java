package edu.one.core.infra.request.filter;

import org.vertx.java.core.http.HttpServerRequest;

public class UserAuthFilter implements Filter {

	@Override
	public boolean verify(HttpServerRequest request) {
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

	@Override
	public void deny(HttpServerRequest request) {
		request.response().setStatusCode(301);
		request.response().putHeader("Location", "http://localhost:8009/login");
		request.response().end();
	}
}
