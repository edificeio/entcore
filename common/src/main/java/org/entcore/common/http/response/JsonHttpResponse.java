/*
 * Copyright © "Open Digital Education", 2014
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

 */

package org.entcore.common.http.response;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.MultiMap;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpFrame;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServerResponse;

public class JsonHttpResponse implements HttpServerResponse {

	private Handler<String> endHandler;
	private String statusMessage;
	private MultiMap headers = MultiMap.caseInsensitiveMultiMap();
	private MultiMap trailers = MultiMap.caseInsensitiveMultiMap();

	public JsonHttpResponse() {
		this(s->{});
	}
	public JsonHttpResponse(Handler<String> handler) {
		this.endHandler = handler;
	}

	@Override
	public HttpServerResponse setWriteQueueMaxSize(int i) {
		return this;
	}

	@Override
	public boolean writeQueueFull() {
		return false;
	}

	@Override
	public HttpServerResponse drainHandler(Handler<Void> voidHandler) {
		return this;
	}

	@Override
	public int getStatusCode() {
		return 0;
	}

	@Override
	public HttpServerResponse setStatusCode(int i) {
		return this;
	}

	@Override
	public String getStatusMessage() {
		return statusMessage;
	}

	@Override
	public HttpServerResponse setStatusMessage(String s) {
		statusMessage = s;
		return this;
	}

	@Override
	public HttpServerResponse setChunked(boolean b) {
		return this;
	}

	@Override
	public boolean isChunked() {
		return false;
	}

	@Override
	public MultiMap headers() {
		return headers;
	}

	@Override
	public HttpServerResponse putHeader(String s, String s2) {
		headers.add(s, s2);
		return this;
	}

	@Override
	public HttpServerResponse putHeader(CharSequence charSequence, CharSequence charSequence2) {
		headers.add(charSequence, charSequence2);
		return this;
	}

	@Override
	public HttpServerResponse putHeader(String s, Iterable<String> strings) {
		headers.add(s, strings);
		return this;
	}

	@Override
	public HttpServerResponse putHeader(CharSequence charSequence, Iterable<CharSequence> charSequences) {
		headers.add(charSequence, charSequences);
		return this;
	}

	@Override
	public MultiMap trailers() {
		return trailers;
	}

	@Override
	public HttpServerResponse putTrailer(String s, String s2) {
		trailers.add(s, s2);
		return this;
	}

	@Override
	public HttpServerResponse putTrailer(CharSequence charSequence, CharSequence charSequence2) {
		trailers.add(charSequence, charSequence2);
		return this;
	}

	@Override
	public HttpServerResponse putTrailer(String s, Iterable<String> strings) {
		trailers.add(s, strings);
		return this;
	}

	@Override
	public HttpServerResponse putTrailer(CharSequence charSequence, Iterable<CharSequence> charSequences) {
		trailers.add(charSequence, charSequences);
		return this;
	}

	@Override
	public HttpServerResponse closeHandler(Handler<Void> voidHandler) {
		return this;
	}

	@Override
	public HttpServerResponse endHandler(Handler<Void> handler) {
		this.endHandler = (s) -> {
			handler.handle(null);
		};
		return this;
	}

	@Override
	public HttpServerResponse write(Buffer buffer) {
		return this;
	}

	@Override
	public HttpServerResponse write(String s, String s2) {
		return this;
	}

	@Override
	public HttpServerResponse write(String s) {
		return this;
	}

	@Override
	public HttpServerResponse writeContinue() {
		return this;
	}

	@Override
	public void end(String s) {
		endHandler.handle(s);
	}

	@Override
	public void end(String s, String s2) {
		endHandler.handle(s);
	}

	@Override
	public void end(Buffer buffer) {
		endHandler.handle(buffer.toString());
	}

	@Override
	public void end() {
		endHandler.handle(null);
	}

	@Override
	public HttpServerResponse sendFile(String s) {
		return this;
	}

	@Override
	public HttpServerResponse sendFile(String filename, long offset) {
		return this;
	}

	@Override
	public HttpServerResponse sendFile(String filename, long offset, long length) {
		return this;
	}

	@Override
	public HttpServerResponse sendFile(String s, Handler<AsyncResult<Void>> asyncResultHandler) {
		return this;
	}

	@Override
	public HttpServerResponse sendFile(String filename, long offset, Handler<AsyncResult<Void>> resultHandler) {
		return this;
	}

	@Override
	public HttpServerResponse sendFile(String filename, long offset, long length,
			Handler<AsyncResult<Void>> resultHandler) {
		return this;
	}

	@Override
	public void close() {

	}

	@Override
	public boolean ended() {
		return false;
	}

	@Override
	public boolean closed() {
		return false;
	}

	@Override
	public boolean headWritten() {
		return false;
	}

	@Override
	public HttpServerResponse headersEndHandler(Handler<Void> handler) {
		return this;
	}

	@Override
	public HttpServerResponse bodyEndHandler(Handler<Void> handler) {
		return this;
	}

	@Override
	public long bytesWritten() {
		return 0;
	}

	@Override
	public int streamId() {
		return 0;
	}

	@Override
	public HttpServerResponse push(HttpMethod method, String path, MultiMap headers,
			Handler<AsyncResult<HttpServerResponse>> handler) {
		return push(method, null, path, headers, handler);
	}

	@Override
	public HttpServerResponse push(io.vertx.core.http.HttpMethod method, String host, String path,
			Handler<AsyncResult<HttpServerResponse>> handler) {
		return push(method, path, handler);
	}

	@Override
	public HttpServerResponse push(HttpMethod method, String path, Handler<AsyncResult<HttpServerResponse>> handler) {
		return push(method, path, null, null, handler);
	}

	@Override
	public HttpServerResponse push(HttpMethod method, String host, String path, MultiMap headers,
			Handler<AsyncResult<HttpServerResponse>> handler) {
		return this;
	}

	@Override
	public HttpServerResponse writeCustomFrame(HttpFrame frame) {
		return this;
	}

	@Override
	public void reset(long code) {
	}

	@Override
	public HttpServerResponse writeCustomFrame(int type, int flags, Buffer payload) {
		return this;
	}

	@Override
	public HttpServerResponse exceptionHandler(Handler<Throwable> throwableHandler) {
		return this;
	}
}
