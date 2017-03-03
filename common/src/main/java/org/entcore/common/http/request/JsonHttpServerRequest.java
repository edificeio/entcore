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
import java.net.URISyntaxException;

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
		return response;
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
		final String uri = object.getString("absoluteURI", object.getString("uri"));
		if (uri != null) {
			try {
				return new URI(uri);
			} catch (URISyntaxException e) {

			}
		}
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
