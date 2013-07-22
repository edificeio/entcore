package edu.one.core.auth;

import edu.one.core.infra.Controller;
import edu.one.core.infra.request.CookieUtils;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import org.vertx.java.core.Handler;
import org.vertx.java.core.VoidHandler;
import org.vertx.java.core.http.HttpServerRequest;
import org.vertx.java.core.json.JsonObject;

public class Auth extends Controller {

	@Override
	public void start() {
		super.start();

		rm.get("/login", new Handler<HttpServerRequest>() {
			@Override
			public void handle(HttpServerRequest request) {
				JsonObject context = new JsonObject();
				context.putString("callBack", request.params().get("callBack"));
				if (request.params().get("error") != null) {
					context.putObject("error", new JsonObject().putString("message", request.params().get("error")));
				}
				renderView(request, context);
			}
		});

		rm.post("/login", new Handler<HttpServerRequest>() {
			@Override
			public void handle(final HttpServerRequest request) {
				request.expectMultiPart(true);
				request.endHandler(new VoidHandler() {
					@Override
					public void handle() {
						String callBack = config.getObject("authenticationServer").getString("loginCallback");
						try {
							if (request.formAttributes().get("callback") != null) {
								callBack =  URLDecoder.decode(request.formAttributes().get("callback"),"UTF-8");
							}
						} catch (UnsupportedEncodingException ex) {
							ex.printStackTrace();
							renderError(request);
						}
						if ("admin".equals(request.formAttributes().get("email"))
								&& "admin".equals(request.formAttributes().get("password"))) {
							CookieUtils.set("oneSessionId", "1234", request.response());
							redirect(request, callBack, "");
						} else {
							try {
								redirect(request, "/login?"
										+ "callback=" + URLEncoder.encode(callBack, "UTF-8")
										+ "&error=auth.error.authenticationFailed");
							} catch (UnsupportedEncodingException ex) {}
						}
					}
				});

			}
		});

		rm.get("/logout", new Handler<HttpServerRequest>() {
			@Override
			public void handle(HttpServerRequest request) {
				renderView(request, config);
			}
		});

	}

}
