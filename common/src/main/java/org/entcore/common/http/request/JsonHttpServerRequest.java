/*
 * Copyright. Tous droits réservés. WebServices pour l’Education.
 */

package org.entcore.common.http.request;

import org.vertx.java.core.Handler;
import org.vertx.java.core.MultiMap;
import org.vertx.java.core.buffer.Buffer;
import org.vertx.java.core.http.*;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.net.NetSocket;

import javax.net.ssl.SSLPeerUnverifiedException;
import javax.security.cert.X509Certificate;
import java.net.InetSocketAddress;
import java.net.URI;

public class JsonHttpServerRequest implements HttpServerRequest {

	private JsonObject object;

	public JsonHttpServerRequest(JsonObject object) {
		this.object = object;
	}

	@Override
	public HttpServerRequest exceptionHandler(Handler<Throwable> throwableHandler) {
		return null;
	}

	@Override
	public HttpVersion version() {
		return null;
	}

	@Override
	public String method() {
		return object.getString("method");
	}

	@Override
	public String uri() {
		return object.getString("uri");
	}

	@Override
	public String path() {
		return object.getString("path");
	}

	@Override
	public String query() {
		return object.getString("query");
	}

	@Override
	public HttpServerResponse response() {
		return null;
	}

	@Override
	public MultiMap headers() {
		MultiMap m = new CaseInsensitiveMultiMap();
		JsonObject h = object.getObject("headers");
		if (h != null) {
			for (String attr : h.getFieldNames()) {
				m.add(attr, h.getString(attr));
			}
		}
		return m;
	}

	@Override
	public MultiMap params() {
		MultiMap m = new CaseInsensitiveMultiMap();
		JsonObject p = object.getObject("params");
		if (p != null) {
			for (String attr : p.getFieldNames()) {
				m.add(attr, p.getString(attr));
			}
		}
		return m;
	}

	@Override
	public InetSocketAddress remoteAddress() {
		return null;
	}

	@Override
	public InetSocketAddress localAddress() {
		return null;
	}

	@Override
	public X509Certificate[] peerCertificateChain() throws SSLPeerUnverifiedException {
		return new X509Certificate[0];
	}

	@Override
	public URI absoluteURI() {
		return null;
	}

	@Override
	public HttpServerRequest bodyHandler(Handler<Buffer> bufferHandler) {
		return null;
	}

	@Override
	public NetSocket netSocket() {
		return null;
	}

	@Override
	public HttpServerRequest expectMultiPart(boolean b) {
		return null;
	}

	@Override
	public HttpServerRequest uploadHandler(Handler<HttpServerFileUpload> httpServerFileUploadHandler) {
		return null;
	}

	@Override
	public MultiMap formAttributes() {
		return null;
	}

	@Override
	public HttpServerRequest dataHandler(Handler<Buffer> bufferHandler) {
		return null;
	}

	@Override
	public HttpServerRequest pause() {
		return null;
	}

	@Override
	public HttpServerRequest resume() {
		return null;
	}

	@Override
	public HttpServerRequest endHandler(Handler<Void> voidHandler) {
		return null;
	}
}
