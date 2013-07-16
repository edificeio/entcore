package edu.one.core.infra.request;

import io.netty.handler.codec.http.Cookie;
import io.netty.handler.codec.http.CookieDecoder;
import io.netty.handler.codec.http.ServerCookieEncoder;
import java.util.Set;
import org.vertx.java.core.http.HttpServerRequest;
import org.vertx.java.core.http.HttpServerResponse;

public class CookieUtils {

	public static String get(String name, HttpServerRequest request) {
		if (request.headers().get("Cookie") != null) {
			Set<Cookie> cookies = CookieDecoder.decode(request.headers().get("Cookie"));
			for (Cookie c : cookies) {
				if (c.getName().equals(name)) {
					return c.getValue();
				}
			}
		}
		return null;
	}

	public static void set(String name, String value, HttpServerResponse response) {
		response.headers().set("Set-Cookie", ServerCookieEncoder.encode(name, value));
	}
}
