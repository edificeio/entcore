package edu.one.core.infra.request;

import io.netty.handler.codec.http.Cookie;
import io.netty.handler.codec.http.CookieDecoder;
import io.netty.handler.codec.http.DefaultCookie;
import io.netty.handler.codec.http.ServerCookieEncoder;

import java.io.UnsupportedEncodingException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Set;

import org.vertx.java.core.http.HttpServerRequest;
import org.vertx.java.core.http.HttpServerResponse;
import org.vertx.java.core.logging.Logger;

import edu.one.core.infra.security.HmacSha1;

public class CookieHelper {

	private String signKey;
	private Logger log;

	private CookieHelper(){}

	private static class CookieHolder {
		private static final CookieHelper instance = new CookieHelper();
	}

	public static CookieHelper getInstance() {
		return CookieHolder.instance;
	}

	public void init(String signkey, Logger log) {
		this.signKey = signkey;
		this.log = log;
	}

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
		set(name, value, Long.MIN_VALUE, response);
	}

	public static void set(String name, String value, long timeout, HttpServerResponse response) {
		set(name, value, timeout, "/", response);
	}

	public static void set(String name, String value, long timeout, String path, HttpServerResponse response) {
		Cookie cookie = new DefaultCookie(name, value);
		cookie.setMaxAge(timeout);
		if (path != null && !path.trim().isEmpty()) {
			cookie.setPath(path);
		}
		response.headers().set("Set-Cookie", ServerCookieEncoder.encode(cookie));
	}

	public void setSigned(String name, String value, long timeout, HttpServerResponse response) {
		setSigned(name, value, timeout, "/", response);
	}

	public void setSigned(String name, String value, long timeout, String path, HttpServerResponse response) {
		Cookie cookie = new DefaultCookie(name, value);
		cookie.setMaxAge(timeout);
		if (path != null && !path.trim().isEmpty()) {
			cookie.setPath(path);
		}
		if (signKey != null) {
			try {
				signCookie(cookie);
			} catch (InvalidKeyException | NoSuchAlgorithmException
					| IllegalStateException | UnsupportedEncodingException e) {
				log.error(e);
				return;
			}
			response.headers().set("Set-Cookie", ServerCookieEncoder.encode(cookie));
		}
	}

	private void signCookie(Cookie cookie)
			throws InvalidKeyException, NoSuchAlgorithmException,
			IllegalStateException, UnsupportedEncodingException {
		String signature = HmacSha1.sign(
				cookie.getDomain()+cookie.getName()+
				cookie.getPath()+cookie.getValue(), signKey);
		cookie.setValue(cookie.getValue() + ":" + signature);
	}

	public String getSigned(String name, HttpServerRequest request) {
		return getSigned(name, "/", request);
	}

	public String getSigned(String name, String path, HttpServerRequest request) {
		if (request.headers().get("Cookie") != null) {
			Set<Cookie> cookies = CookieDecoder.decode(request.headers().get("Cookie"));
			for (Cookie c : cookies) {
				if (c.getName().equals(name) && c.getValue().contains(":")) {
					int idx = c.getValue().lastIndexOf(":");
					if (idx > c.getValue().length() - 1) continue;
					String value = c.getValue().substring(0, idx);
					String signature = c.getValue().substring(idx+1);
					String calcSign = null;
					String cookiePath = path;
					if (cookiePath == null || cookiePath.trim().isEmpty()) {
						cookiePath = c.getPath();
					}
					try {
						calcSign = HmacSha1.sign(
								c.getDomain()+c.getName()+
								cookiePath+value, signKey);
					} catch (InvalidKeyException | NoSuchAlgorithmException
							| IllegalStateException
							| UnsupportedEncodingException e) {
					}
					if (calcSign != null && calcSign.equals(signature)) {
						return value;
					}
				}
			}
		}
		return null;
	}
}
