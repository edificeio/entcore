package edu.one.core.infra.security.oauth;

import org.vertx.java.core.Handler;

import edu.one.core.infra.security.SecureHttpServerRequest;

public interface OAuthResourceProvider {

	void validToken(SecureHttpServerRequest request, Handler<Boolean> handler);

}
