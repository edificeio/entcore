/*
 * Copyright © WebServices pour l'Éducation, 2014
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
 */

package org.entcore.common.http.response;

import org.vertx.java.core.AsyncResult;
import org.vertx.java.core.Handler;
import org.vertx.java.core.MultiMap;
import org.vertx.java.core.buffer.Buffer;
import org.vertx.java.core.http.HttpServerResponse;

public class JsonHttpResponse implements HttpServerResponse {

	private Handler<String> endHandler;

	public JsonHttpResponse(Handler<String> handler) {
		this.endHandler = handler;
	}

	@Override
	public HttpServerResponse setWriteQueueMaxSize(int i) {
		return null;
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
		return null;
	}

	@Override
	public HttpServerResponse setStatusMessage(String s) {
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
		return null;
	}

	@Override
	public HttpServerResponse putHeader(String s, String s2) {
		return null;
	}

	@Override
	public HttpServerResponse putHeader(CharSequence charSequence, CharSequence charSequence2) {
		return null;
	}

	@Override
	public HttpServerResponse putHeader(String s, Iterable<String> strings) {
		return null;
	}

	@Override
	public HttpServerResponse putHeader(CharSequence charSequence, Iterable<CharSequence> charSequences) {
		return null;
	}

	@Override
	public MultiMap trailers() {
		return null;
	}

	@Override
	public HttpServerResponse putTrailer(String s, String s2) {
		return null;
	}

	@Override
	public HttpServerResponse putTrailer(CharSequence charSequence, CharSequence charSequence2) {
		return null;
	}

	@Override
	public HttpServerResponse putTrailer(String s, Iterable<String> strings) {
		return null;
	}

	@Override
	public HttpServerResponse putTrailer(CharSequence charSequence, Iterable<CharSequence> charSequences) {
		return null;
	}

	@Override
	public HttpServerResponse closeHandler(Handler<Void> voidHandler) {
		return null;
	}

	@Override
	public HttpServerResponse write(Buffer buffer) {
		return null;
	}

	@Override
	public HttpServerResponse write(String s, String s2) {
		return null;
	}

	@Override
	public HttpServerResponse write(String s) {
		return null;
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
		return null;
	}

	@Override
	public HttpServerResponse sendFile(String s, String s2) {
		return null;
	}

	@Override
	public HttpServerResponse sendFile(String s, Handler<AsyncResult<Void>> asyncResultHandler) {
		return null;
	}

	@Override
	public HttpServerResponse sendFile(String s, String s2, Handler<AsyncResult<Void>> asyncResultHandler) {
		return null;
	}

	@Override
	public void close() {

	}

	@Override
	public HttpServerResponse exceptionHandler(Handler<Throwable> throwableHandler) {
		return null;
	}
}
