package edu.one.core.infra.security.resources;

import org.vertx.java.core.Handler;
import org.vertx.java.core.http.HttpServerRequest;

public class ResoucesAccess {

	private ResourcesProvider provider;

	public ResoucesAccess(ResourcesProvider provider) {
		this.provider = provider;
	}

	public void authorize(final HttpServerRequest resourceRequest, final Handler<Boolean> handler) {
		getUserInfos(resourceRequest, new Handler<UserInfos>() {

			@Override
			public void handle(UserInfos user) {
				if (user != null) {
					provider.authorize(resourceRequest, user, handler);
				} else {
					handler.handle(false);
				}
			}
		});
	}

	private void getUserInfos(HttpServerRequest resourceRequest, Handler<UserInfos> handler) {
		// TODO replace this mock
		handler.handle(new UserInfos("blip"));
	}

}
