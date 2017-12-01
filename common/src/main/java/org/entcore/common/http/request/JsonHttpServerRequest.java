/* Copyright © WebServices pour l'Éducation, 2014
 *
 * This file is part of ENT Core. ENT Core is a versatile ENT engine based on the JVM.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation (version 3 of the License).
 *
 * For the sake of explanation, any module that communicate over native
 * Web protocols, such as HTTP, with ENT Core is outside the scope of this
 * license and could be license under its own terms. This is merely considered
 * normal use of ENT Core, and does not fall under the heading of "covered work".
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *
 */

package org.entcore.common.http.request;

import fr.wseduc.webutils.http.Renders;
import io.vertx.core.Handler;
import io.vertx.core.MultiMap;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.*;
import io.vertx.core.json.JsonObject;
import io.vertx.core.net.NetSocket;
import io.vertx.core.net.SocketAddress;

import javax.net.ssl.SSLPeerUnverifiedException;
import javax.net.ssl.SSLSession;
import javax.security.cert.X509Certificate;

public class JsonHttpServerRequest implements HttpServerRequest {

	private JsonObject object;
	private HttpServerResponse response;

	public JsonHttpServerRequest(JsonObject object) {
		this.object = object;
	}

	public JsonHttpServerRequest(JsonObject object, HttpServerResponse response) {
		this.object = object;
		this.response = response;
	}

	@Override
	public HttpServerRequest exceptionHandler(Handler<Throwable> throwableHandler) {
		return null;
	}

	@Override
	public HttpServerRequest handler(Handler<Buffer> handler) {
		return null;
	}

	@Override
	public HttpVersion version() {
		return null;
	}

	@Override
	public HttpMethod method() {
		return HttpMethod.valueOf(object.getString("method", "").toUpperCase());
	}

	@Override
	public String rawMethod() {
		return object.getString("method");
	}

	@Override
	public boolean isSSL() {
		return object.getBoolean("ssl", false);
	}

	@Override
	public String scheme() {
		return object.getString("scheme");
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
	public String host() {
		return Renders.getHost(this);
	}

	@Override
	public HttpServerResponse response() {
		return response;
	}

	@Override
	public MultiMap headers() {
		MultiMap m = new CaseInsensitiveHeaders();
		JsonObject h = object.getJsonObject("headers");
		if (h != null) {
			for (String attr : h.fieldNames()) {
				m.add(attr, h.getString(attr));
			}
		}
		return m;
	}

	@Override
	public String getHeader(String headerName) {
		return null;
	}

	@Override
	public String getHeader(CharSequence headerName) {
		return null;
	}

	@Override
	public MultiMap params() {
		MultiMap m = new CaseInsensitiveHeaders();
		JsonObject p = object.getJsonObject("params");
		if (p != null) {
			for (String attr : p.fieldNames()) {
				m.add(attr, p.getString(attr));
			}
		}
		return m;
	}

	@Override
	public String getParam(String paramName) {
		return null;
	}

	@Override
	public SocketAddress remoteAddress() {
		return null;
	}

	@Override
	public SocketAddress localAddress() {
		return null;
	}

	@Override
	public SSLSession sslSession() {
		return null;
	}

	@Override
	public X509Certificate[] peerCertificateChain() throws SSLPeerUnverifiedException {
		return new X509Certificate[0];
	}

	@Override
	public String absoluteURI() {
		return object.getString("absoluteURI", object.getString("uri"));
	}

	@Override
	public HttpServerRequest bodyHandler(Handler<Buffer> bufferHandler) {
		return this;
	}

	@Override
	public NetSocket netSocket() {
		return null;
	}

	@Override
	public HttpServerRequest setExpectMultipart(boolean b) {
		return null;
	}

	@Override
	public boolean isExpectMultipart() {
		return false;
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
	public String getFormAttribute(String attributeName) {
		return null;
	}

	@Override
	public ServerWebSocket upgrade() {
		return null;
	}

	@Override
	public boolean isEnded() {
		return false;
	}

	@Override
	public HttpServerRequest customFrameHandler(Handler<HttpFrame> handler) {
		return null;
	}

	@Override
	public HttpConnection connection() {
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
