package edu.one.core.infra.security;

import java.net.InetSocketAddress;
import java.net.URI;

import javax.net.ssl.SSLPeerUnverifiedException;
import javax.security.cert.X509Certificate;

import org.vertx.java.core.Handler;
import org.vertx.java.core.MultiMap;
import org.vertx.java.core.buffer.Buffer;
import org.vertx.java.core.http.HttpServerFileUpload;
import org.vertx.java.core.http.HttpServerRequest;
import org.vertx.java.core.http.HttpServerResponse;
import org.vertx.java.core.http.HttpVersion;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.net.NetSocket;

public class SecureHttpServerRequest implements HttpServerRequest {

	private final HttpServerRequest request;
	private JsonObject session;

	public SecureHttpServerRequest(HttpServerRequest request) {
		this.request = request;
	}

	@Override
	public HttpServerRequest dataHandler(Handler<Buffer> handler) {
		request.dataHandler(handler);
		return this;
	}

	@Override
	public HttpServerRequest pause() {
		request.pause();
		return this;
	}

	@Override
	public HttpServerRequest resume() {
		request.resume();
		return this;
	}

	@Override
	public HttpServerRequest endHandler(Handler<Void> endHandler) {
		request.endHandler(endHandler);
		return this;
	}

	@Override
	public HttpServerRequest exceptionHandler(Handler<Throwable> handler) {
		request.exceptionHandler(handler);
		return this;
	}

	@Override
	public HttpVersion version() {
		return request.version();
	}

	@Override
	public String method() {
		return request.method();
	}

	@Override
	public String uri() {
		return request.uri();
	}

	@Override
	public String path() {
		return request.path();
	}

	@Override
	public String query() {
		return request.query();
	}

	@Override
	public HttpServerResponse response() {
		return request.response();
	}

	@Override
	public MultiMap headers() {
		return request.headers();
	}

	@Override
	public MultiMap params() {
		return request.params();
	}

	@Override
	public InetSocketAddress remoteAddress() {
		return request.remoteAddress();
	}

	@Override
	public X509Certificate[] peerCertificateChain() throws SSLPeerUnverifiedException {
		return request.peerCertificateChain();
	}

	@Override
	public URI absoluteURI() {
		return request.absoluteURI();
	}

	@Override
	public HttpServerRequest bodyHandler(Handler<Buffer> bodyHandler) {
		request.bodyHandler(bodyHandler);
		return this;
	}

	@Override
	public NetSocket netSocket() {
		return request.netSocket();
	}

	@Override
	public HttpServerRequest expectMultiPart(boolean expect) {
		request.expectMultiPart(expect);
		return this;
	}

	@Override
	public HttpServerRequest uploadHandler(Handler<HttpServerFileUpload> uploadHandler) {
		request.uploadHandler(uploadHandler);
		return this;
	}

	@Override
	public MultiMap formAttributes() {
		return request.formAttributes();
	}

	public JsonObject getSession() {
		return session;
	}

	public void setSession(JsonObject session) {
		this.session = session;
	}

}
