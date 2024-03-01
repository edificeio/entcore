package org.entcore.common.s3.utils;

import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.MultiMap;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.*;
import io.vertx.core.net.NetSocket;

import java.util.List;

public class ErrorHttpClientResponse implements HttpClientResponse {

	private final int statusCode;
	private final String statusMessage;

	public ErrorHttpClientResponse(int statusCode, String statusMessage) {
		this.statusCode = statusCode;
		this.statusMessage = statusMessage;
	}

	@Override
	public HttpClientResponse exceptionHandler(Handler<Throwable> handler) {
		return null;
	}

	@Override
	public int statusCode() {
		return statusCode;
	}

	@Override
	public String statusMessage() {
		return statusMessage;
	}

	@Override
	public MultiMap headers() {
		return null;
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
	public String getTrailer(String trailerName) {
		return null;
	}

	@Override
	public MultiMap trailers() {
		return null;
	}

	@Override
	public List<String> cookies() {
		return null;
	}

	@Override
	public HttpClientResponse bodyHandler(Handler<Buffer> handler) {
		return null;
	}

	@Override
	public Future<Buffer> body() {
		return null;
	}

	@Override
	public Future<Void> end() {
		return null;
	}

	@Override
	public HttpClientResponse customFrameHandler(Handler<HttpFrame> handler) {
		return null;
	}

	@Override
	public NetSocket netSocket() {
		return null;
	}

	@Override
	public HttpClientRequest request() {
		return null;
	}

	@Override
	public HttpClientResponse streamPriorityHandler(Handler<StreamPriority> handler) {
		return null;
	}

	@Override
	public HttpClientResponse endHandler(Handler<Void> handler) {
		return null;
	}

	@Override
	public HttpVersion version() {
		return null;
	}

	@Override
	public HttpClientResponse handler(Handler<Buffer> handler) {
		return null;
	}

	@Override
	public HttpClientResponse pause() {
		return null;
	}

	@Override
	public HttpClientResponse fetch(long amount) {
		return null;
	}

	@Override
	public HttpClientResponse resume() {
		return null;
	}

}
