/* Copyright © "Open Digital Education", 2014
 *
 * This program is published by "Open Digital Education".
 * You must indicate the name of the software and the company in any production /contribution
 * using the software and indicate on the home page of the software industry in question,
 * "powered by Open Digital Education" with a reference to the website: https://opendigitaleducation.com/.
 *
 * This program is free software, licensed under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation, version 3 of the License.
 *
 * You can redistribute this application and/or modify it since you respect the terms of the GNU Affero General Public License.
 * If you modify the source code and then use this modified source code in your creation, you must make available the source code of your modifications.
 *
 * You should have received a copy of the GNU Affero General Public License along with the software.
 * If not, please see : <http://www.gnu.org/licenses/>. Full compliance requires reading the terms of this license and following its directives.

 *
 */

package org.entcore.common.http.request;

import fr.wseduc.webutils.http.Renders;
import io.netty.handler.codec.DecoderResult;
import io.vertx.codegen.annotations.Nullable;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.MultiMap;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.Cookie;
import io.vertx.core.http.HttpConnection;
import io.vertx.core.http.HttpFrame;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServerFileUpload;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.http.HttpVersion;
import io.vertx.core.http.ServerWebSocket;
import io.vertx.core.http.StreamPriority;
import io.vertx.core.json.JsonObject;
import io.vertx.core.net.HostAndPort;
import io.vertx.core.net.NetSocket;
import io.vertx.core.net.SocketAddress;
import org.apache.commons.lang3.NotImplementedException;

import javax.net.ssl.SSLPeerUnverifiedException;
import javax.net.ssl.SSLSession;
import javax.security.cert.X509Certificate;
import java.util.Map;
import java.util.Set;

public class JsonHttpServerRequest implements HttpServerRequest {

	private JsonObject object;
	private HttpServerResponse response;

	public JsonHttpServerRequest(JsonObject object) {
		this.object = object;
	}

	public JsonHttpServerRequest(JsonObject object, MultiMap headers) {
		final JsonObject h = new JsonObject();
		if(headers!=null) {
			headers.names().forEach( header -> {
				h.put( header, headers.get(header) );
			});
		}
		this.object = object.put("headers", h);
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
	public @Nullable HostAndPort authority() {
		return null;
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
		MultiMap m = MultiMap.caseInsensitiveMultiMap();
		JsonObject h = object.getJsonObject("headers");
		if (h != null) {
			for (String attr : h.fieldNames()) {
				final String val = h.getString(attr);
				if(val != null) {
					m.add(attr, val);
				}
			}
		}
		return m;
	}

	@Override
	public String getHeader(String headerName) {
		final MultiMap headers = headers();
		return headers == null ? null : headers.get(headerName);
	}

	@Override
	public String getHeader(CharSequence headerName) {
		return getHeader(headerName.toString());
	}

	@Override
	public HttpServerRequest setParamsCharset(String charset) {
		return this;
	}

	@Override
	public String getParamsCharset() {
		return null;
	}

	@Override
	public MultiMap params() {
		MultiMap m = MultiMap.caseInsensitiveMultiMap();
		JsonObject p = object.getJsonObject("params");
		if (p != null) {
			for (String attr : p.fieldNames()) {
				final String val = p.getString(attr);
				if(val != null) {
					m.add(attr, val);
				}
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
	public Future<Buffer> body() {
		return Future.succeededFuture();
	}

	@Override
	public Future<Void> end() {
		return Future.succeededFuture();
	}

	@Override
	public Future<NetSocket> toNetSocket() {
		return Future.failedFuture(new NotImplementedException("toNetSocket"));
	}

	@Override
	public HttpServerRequest setExpectMultipart(boolean b) {
		return this;
	}

	@Override
	public boolean isExpectMultipart() {
		return false;
	}

	@Override
	public HttpServerRequest uploadHandler(Handler<HttpServerFileUpload> httpServerFileUploadHandler) {
		return this;
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
	public Future<ServerWebSocket> toWebSocket() {
		return Future.failedFuture(new NotImplementedException("toWebSocket"));
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

	@Override
	public HttpServerRequest streamPriorityHandler(Handler<StreamPriority> handler)
	{
		return null;
	}

	@Override
	public DecoderResult decoderResult() {
		return null;
	}

	@Override
	public long bytesRead()
	{
		return -1;
	}

	@Override
	public HttpServerRequest fetch(long bytes)
	{
		return null;
	}

	@Override
	public Map<String, Cookie> cookieMap()
	{
		return null;
	}

	@Override
	public Set<Cookie> cookies(String name) {
		return null;
	}

	@Override
	public Set<Cookie> cookies() {
		return null;
	}

	@Override
	public int cookieCount()
	{
		return -1;
	}

	@Override
	public Cookie getCookie(String str)
	{
		return null;
	}

	@Override
	public @Nullable Cookie getCookie(String name, String domain, String path) {
		return null;
	}
}
