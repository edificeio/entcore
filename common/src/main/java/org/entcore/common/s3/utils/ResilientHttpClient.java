package org.entcore.common.s3.utils;

import io.vertx.core.*;
import io.vertx.core.http.*;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.core.net.SocketAddress;
import io.vertx.core.streams.ReadStream;

import java.net.URI;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

public class ResilientHttpClient implements HttpClient {

	private static final Logger log = LoggerFactory.getLogger(ResilientHttpClient.class);

	private final Vertx vertx;
	private HttpClient httpClient;
	private final int timeout;
	private final int threshold;
	private final long openDelay;
	private final URI uri;
	private final boolean keepAlive;
	private AtomicInteger errorsCount = new AtomicInteger(0);
	private AtomicBoolean closedCircuit = new AtomicBoolean(false);
	private Handler<HalfOpenResult> halfOpenHandler;

	public ResilientHttpClient(Vertx vertx, URI uri, boolean keepAlive, int timeout, int threshold, long openDelay) {
		this.vertx = vertx;
		this.timeout = timeout;
		this.threshold = threshold;
		this.openDelay = openDelay;
		this.uri = uri;
		this.keepAlive = keepAlive;
		reconfigure();
	}

	@Override
	public HttpClient websocket(RequestOptions options, Handler<WebSocket> wsConnect) {
		return httpClient.websocket(options, wsConnect);
	}

	@Override
	public HttpClient websocket(int port, String host, String requestURI, Handler<WebSocket> wsConnect) {
		return httpClient.websocket(port, host, requestURI, wsConnect);
	}

	@Override
	public HttpClient websocket(RequestOptions options, Handler<WebSocket> wsConnect, Handler<Throwable> failureHandler) {
		return httpClient.websocket(options, wsConnect, failureHandler);
	}

	public HttpClient websocket(int port, String host, String requestURI, Handler<WebSocket> wsConnect, Handler<Throwable> failureHandler){
		return httpClient.websocket(port, host, requestURI, wsConnect, failureHandler);
	}

	@Override
	public HttpClient websocket(String host, String requestURI, Handler<WebSocket> wsConnect) {
		return httpClient.websocket(host, requestURI, wsConnect);
	}

	@Override
	public HttpClient websocket(String host, String requestURI, Handler<WebSocket> wsConnect, Handler<Throwable> failureHandler) {
		return httpClient.websocket(host, requestURI, wsConnect, failureHandler);
	}

	@Override
	public HttpClient websocket(RequestOptions options, MultiMap headers, Handler<WebSocket> wsConnect) {
		return httpClient.websocket(options, headers, wsConnect);
	}

	@Override
	public HttpClient websocket(int port, String host, String requestURI, MultiMap headers, Handler<WebSocket> wsConnect) {
		return httpClient.websocket(port, host, requestURI, headers, wsConnect);
	}

	@Override
	public HttpClient websocket(RequestOptions options, MultiMap headers, Handler<WebSocket> wsConnect, Handler<Throwable> failureHandler) {
		return httpClient.websocket(options, headers, wsConnect, failureHandler);
	}

	@Override
	public HttpClient websocket(int port, String host, String requestURI, MultiMap headers, Handler<WebSocket> wsConnect, Handler<Throwable> failureHandler) {
		return httpClient.websocket(port, host, requestURI, headers, wsConnect, failureHandler);
	}

	@Override
	public HttpClient websocket(String host, String requestURI, MultiMap headers, Handler<WebSocket> wsConnect) {
		return httpClient.websocket(host, requestURI, headers, wsConnect);
	}

	@Override
	public HttpClient websocket(String host, String requestURI, MultiMap headers, Handler<WebSocket> wsConnect, Handler<Throwable> failureHandler) {
		return httpClient.websocket(host, requestURI, headers, wsConnect, failureHandler);
	}

	@Override
	public HttpClient websocket(RequestOptions options, MultiMap headers, WebsocketVersion version, Handler<WebSocket> wsConnect) {
		return httpClient.websocket(options, headers, version, wsConnect);
	}

	@Override
	public HttpClient websocket(int port, String host, String requestURI, MultiMap headers, WebsocketVersion version, Handler<WebSocket> wsConnect) {
		return httpClient.websocket(port, host, requestURI, headers, version, wsConnect);
	}

	@Override
	public HttpClient websocket(RequestOptions options, MultiMap headers, WebsocketVersion version, Handler<WebSocket> wsConnect, Handler<Throwable> failureHandler) {
		return websocket(options, headers, version, null, wsConnect, failureHandler);
	}

	@Override
	public HttpClient websocket(int port, String host, String requestURI, MultiMap headers, WebsocketVersion version
			, Handler<WebSocket> wsConnect, Handler<Throwable> failureHandler) {
		return httpClient.websocket(port, host, requestURI, headers, version, wsConnect, failureHandler);
	}

	@Override
	public HttpClient websocket(String host, String requestURI, MultiMap headers, WebsocketVersion version, Handler<WebSocket> wsConnect) {
		return httpClient.websocket(host, requestURI, headers, version, wsConnect);
	}

	@Override
	public HttpClient websocket(String host, String requestURI, MultiMap headers, WebsocketVersion version
			, Handler<WebSocket> wsConnect, Handler<Throwable> failureHandler) {
		return httpClient.websocket(host, requestURI, headers, version, wsConnect, failureHandler);
	}

	@Override
	public HttpClient websocket(RequestOptions options, MultiMap headers, WebsocketVersion version, String subProtocols, Handler<WebSocket> wsConnect) {
		return httpClient.websocket(options, headers, version, subProtocols, wsConnect);
	}

	@Override
	public HttpClient websocket(int port, String host, String requestURI, MultiMap headers, WebsocketVersion version,
								String subProtocols, Handler<WebSocket> wsConnect) {
		return httpClient.websocket(port, host, requestURI, headers, version, subProtocols, wsConnect);
	}

	@Override
	public HttpClient websocketAbs(String url, MultiMap headers, WebsocketVersion version, String subProtocols, Handler<WebSocket> wsConnect, Handler<Throwable> failureHandler) {
		return httpClient.websocket(url, headers, version, subProtocols, wsConnect, failureHandler);
	}

	@Override
	public HttpClient websocket(RequestOptions options, MultiMap headers, WebsocketVersion version, String subProtocols, Handler<WebSocket> wsConnect, Handler<Throwable> failureHandler) {
		return httpClient.websocket(options, headers, version, subProtocols, wsConnect, failureHandler);
	}

	@Override
	public HttpClient websocket(int port, String host, String requestURI, MultiMap headers, WebsocketVersion version,
								String subProtocols, Handler<WebSocket> wsConnect, Handler<Throwable> failureHandler) {
		return httpClient.websocket(port, host, requestURI, headers, version, wsConnect, failureHandler);
	}

	@Override
	public HttpClient websocket(String host, String requestURI, MultiMap headers, WebsocketVersion version, String subProtocols, Handler<WebSocket> wsConnect) {
		return httpClient.websocket(host, requestURI, headers, version, subProtocols, wsConnect);
	}

	@Override
	public HttpClient websocket(String host, String requestURI, MultiMap headers, WebsocketVersion version, String subProtocols
			, Handler<WebSocket> wsConnect, Handler<Throwable> failureHandler) {
		return httpClient.websocket(host, requestURI, headers, version, subProtocols, wsConnect, failureHandler);
	}

	@Override
	public HttpClient websocket(String requestURI, Handler<WebSocket> wsConnect) {
		return httpClient.websocket(requestURI, wsConnect);
	}

	@Override
	public HttpClient websocket(String requestURI, Handler<WebSocket> wsConnect, Handler<Throwable> failureHandler) {
		return httpClient.websocket(requestURI, wsConnect, failureHandler);
	}

	@Override
	public HttpClient websocket(String requestURI, MultiMap headers, Handler<WebSocket> wsConnect) {
		return httpClient.websocket(requestURI, headers, wsConnect);
	}

	@Override
	public HttpClient websocket(String requestURI, MultiMap headers, Handler<WebSocket> wsConnect, Handler<Throwable> failureHandler) {
		return httpClient.websocket(requestURI, headers, wsConnect, failureHandler);
	}

	@Override
	public HttpClient websocket(String requestURI, MultiMap headers, WebsocketVersion version, Handler<WebSocket> wsConnect) {
		return httpClient.websocket(requestURI, headers, version, wsConnect);
	}

	@Override
	public HttpClient websocket(String requestURI, MultiMap headers, WebsocketVersion version, Handler<WebSocket> wsConnect, Handler<Throwable> failureHandler) {
		return httpClient.websocket(requestURI, headers, version, wsConnect, failureHandler);
	}

	@Override
	public HttpClient websocket(String requestURI, MultiMap headers, WebsocketVersion version, String subProtocols, Handler<WebSocket> wsConnect) {
		return httpClient.websocket(requestURI, headers, version, subProtocols, wsConnect);
	}
	@Override
	public HttpClient websocket(String requestURI, MultiMap headers, WebsocketVersion version, String subProtocols
			, Handler<WebSocket> wsConnect, Handler<Throwable> failureHandler) {
		return httpClient.websocket(requestURI, headers, version, wsConnect, failureHandler);
	}

	@Override
	public void webSocket(int port, String host, String requestURI, Handler<AsyncResult<WebSocket>> handler) {
		httpClient.webSocket(port, host, requestURI, handler);
	}

	@Override
	public void webSocket(String host, String requestURI, Handler<AsyncResult<WebSocket>> handler) {
		httpClient.webSocket(host, requestURI, handler);
	}

	@Override
	public void webSocket(String requestURI, Handler<AsyncResult<WebSocket>> handler) {
		httpClient.webSocket(requestURI, handler);
	}

	@Override
	public void webSocket(WebSocketConnectOptions options, Handler<AsyncResult<WebSocket>> handler) {
		httpClient.webSocket(options, handler);
	}

	@Override
	public void webSocketAbs(String url, MultiMap headers, WebsocketVersion version, List<String> subProtocols, Handler<AsyncResult<WebSocket>> handler) {
		httpClient.webSocketAbs(url, headers, version, subProtocols, handler);
	}

	@Override
	public ReadStream<WebSocket> websocketStream(RequestOptions options) {
		return websocketStream(options, null);
	}

	@Override
	public ReadStream<WebSocket> websocketStream(int port, String host, String requestURI) {
		return websocketStream(port, host, requestURI, null, null);
	}

	@Override
	public ReadStream<WebSocket> websocketStream(String host, String requestURI) {
		return httpClient.websocketStream(host, requestURI);
	}

	@Override
	public ReadStream<WebSocket> websocketStream(RequestOptions options, MultiMap headers) {
		return websocketStream(options, headers, null);
	}

	@Override
	public ReadStream<WebSocket> websocketStream(int port, String host, String requestURI, MultiMap headers) {
		return websocketStream(port, host, requestURI, headers, null);
	}

	@Override
	public ReadStream<WebSocket> websocketStream(String host, String requestURI, MultiMap headers) {
		return httpClient.websocketStream(host, requestURI, headers);
	}

	@Override
	public ReadStream<WebSocket> websocketStream(RequestOptions options, MultiMap headers, WebsocketVersion version) {
		return websocketStream(options, headers, version, null);
	}

	@Override
	public ReadStream<WebSocket> websocketStream(int port, String host, String requestURI, MultiMap headers, WebsocketVersion version) {
		return websocketStream(port, host, requestURI, headers, version, null);
	}

	@Override
	public ReadStream<WebSocket> websocketStream(String host, String requestURI, MultiMap headers, WebsocketVersion version) {
		return httpClient.websocketStream(host, requestURI, headers, version);
	}

	@Override
	public ReadStream<WebSocket> websocketStreamAbs(String url, MultiMap headers, WebsocketVersion version, String subProtocols) {
		return httpClient.websocketStreamAbs(url, headers, version, subProtocols);
	}

	@Override
	public ReadStream<WebSocket> websocketStream(RequestOptions options, MultiMap headers, WebsocketVersion version, String subProtocols) {
		return httpClient.websocketStream(options, headers, version, subProtocols);
	}

	@Override
	public ReadStream<WebSocket> websocketStream(int port, String host, String requestURI, MultiMap headers, WebsocketVersion version,
										   String subProtocols) {
		return httpClient.websocketStream(port, host, requestURI, headers, version, subProtocols);
	}

	@Override
	public ReadStream<WebSocket> websocketStream(String host, String requestURI, MultiMap headers, WebsocketVersion version, String subProtocols) {
		return httpClient.websocketStream(host, requestURI, headers, version, subProtocols);
	}

	@Override
	public ReadStream<WebSocket> websocketStream(String requestURI) {
		return httpClient.websocketStream(requestURI);
	}

	@Override
	public ReadStream<WebSocket> websocketStream(String requestURI, MultiMap headers) {
		return httpClient.websocketStream(requestURI, headers);
	}

	@Override
	public ReadStream<WebSocket> websocketStream(String requestURI, MultiMap headers, WebsocketVersion version) {
		return httpClient.websocketStream(requestURI, headers, version);
	}

	@Override
	public ReadStream<WebSocket> websocketStream(String requestURI, MultiMap headers, WebsocketVersion version, String subProtocols) {
		return httpClient.websocketStream(requestURI, headers, version, subProtocols);
	}

	@Override
	public HttpClient connectionHandler(Handler<HttpConnection> handler) {
		return httpClient.connectionHandler(handler);
	}

	@Override
	public HttpClientRequest requestAbs(HttpMethod method, String absoluteURI, Handler<HttpClientResponse> handler) {
		if (httpClient == null) {
			handler.handle(new ErrorHttpClientResponse(500, ""));
			return null;
		}
		final HttpClientRequest req = httpClient.requestAbs(method, absoluteURI, handler);
		preConfigureRequest(handler, req);
		return req;
	}

	@Override
	public HttpClientRequest requestAbs(HttpMethod method, SocketAddress serverAddress, String absoluteURI, Handler<HttpClientResponse> responseHandler) {
		return httpClient.requestAbs(method, serverAddress, absoluteURI, responseHandler);
	}

	@Override
	public HttpClientRequest get(RequestOptions options) {
		if (httpClient == null) {
			return null;
		}
		final HttpClientRequest req = httpClient.get(options);
		preConfigureRequest(null, req);
		return req;
	}

	@Override
	public HttpClientRequest request(HttpMethod method, int port, String host, String requestURI, Handler<HttpClientResponse> handler) {
		if (httpClient == null) {
			handler.handle(new ErrorHttpClientResponse(500, ""));
			return null;
		}
		final HttpClientRequest req = httpClient.request(method, port, host, requestURI, handler);
		preConfigureRequest(handler, req);
		return req;
	}

	@Override
	public HttpClientRequest request(HttpMethod method, SocketAddress serverAddress, int port, String host, String requestURI, Handler<HttpClientResponse> responseHandler) {
		return httpClient.request(method, serverAddress, port, host, requestURI, responseHandler);
	}

	@Override
	public HttpClientRequest request(HttpMethod method, String host, String requestURI, Handler<HttpClientResponse> handler) {
		if (httpClient == null) {
			handler.handle(new ErrorHttpClientResponse(500, ""));
			return null;
		}
		final HttpClientRequest req = httpClient.request(method, host, requestURI, handler);
		preConfigureRequest(handler, req);
		return req;
	}

	@Override
	public HttpClientRequest request(HttpMethod method, String requestURI) {
		if (httpClient == null) {
			return null;
		}
		final HttpClientRequest req = httpClient.request(method, requestURI);
		preConfigureRequest(null, req);
		return req;
	}

	@Override
	public HttpClientRequest request(HttpMethod method, String requestURI, Handler<HttpClientResponse> responseHandler) {
		if (httpClient == null) {
			responseHandler.handle(new ErrorHttpClientResponse(500, ""));
			return null;
		}
		final HttpClientRequest req = httpClient.request(method, requestURI, responseHandler);
		preConfigureRequest(responseHandler, req);
		return req;
	}

	@Override
	public HttpClientRequest requestAbs(HttpMethod method, String absoluteURI) {
		if (httpClient == null) {
			return null;
		}
		final HttpClientRequest req = httpClient.requestAbs(method, absoluteURI);
		preConfigureRequest(null, req);
		return req;
	}

	@Override
	public HttpClientRequest requestAbs(HttpMethod method, SocketAddress serverAddress, String absoluteURI) {
		return httpClient.requestAbs(method, serverAddress, absoluteURI);
	}

	@Override
	public HttpClientRequest request(HttpMethod method, int port, String host, String requestURI) {
		if (httpClient == null) {
			return null;
		}
		final HttpClientRequest req = httpClient.request(method, port, host, requestURI);
		preConfigureRequest(null, req);
		return req;
	}

	@Override
	public HttpClientRequest request(HttpMethod method, SocketAddress serverAddress, int port, String host, String requestURI) {
		return httpClient.request(method, serverAddress, port, host, requestURI);
	}

	@Override
	public HttpClientRequest request(HttpMethod method, RequestOptions options, Handler<HttpClientResponse> responseHandler) {
		if (httpClient == null) {
			responseHandler.handle(new ErrorHttpClientResponse(500, ""));
			return null;
		}
		final HttpClientRequest req = httpClient.request(method, options, responseHandler);
		preConfigureRequest(responseHandler, req);
		return req;
	}

	@Override
	public HttpClientRequest request(HttpMethod method, SocketAddress serverAddress, RequestOptions options, Handler<HttpClientResponse> responseHandler) {
		return httpClient.request(method, serverAddress, options, responseHandler);
	}

	@Override
	public HttpClientRequest request(HttpMethod method, SocketAddress serverAddress, RequestOptions options) {
		return httpClient.request(method, serverAddress, options);
	}

	@Override
	public HttpClientRequest request(HttpMethod method, RequestOptions options) {
		if (httpClient == null) {
			return null;
		}
		final HttpClientRequest req = httpClient.request(method, options);
		preConfigureRequest(null, req);
		return req;
	}

	@Override
	public HttpClientRequest request(HttpMethod method, String host, String requestURI) {
		if (httpClient == null) {
			return null;
		}
		final HttpClientRequest req = httpClient.request(method, host, requestURI);
		preConfigureRequest(null, req);
		return req;
	}

	@Override
	public HttpClientRequest get(int port, String host, String requestURI) {
		if (httpClient == null) {
			return null;
		}
		final HttpClientRequest req = httpClient.get(port, host, requestURI);
		preConfigureRequest(null, req);
		return req;
	}

	@Override
	public HttpClientRequest get(String host, String requestURI) {
		if (httpClient == null) {
			return null;
		}
		final HttpClientRequest req = httpClient.get(host, requestURI);
		preConfigureRequest(null, req);
		return req;
	}

	@Override
	public HttpClientRequest get(RequestOptions options, Handler<HttpClientResponse> responseHandler) {
		return request(HttpMethod.GET, options, responseHandler);
	}

	@Override
	public HttpClientRequest get(int port, String host, String requestURI, Handler<HttpClientResponse> responseHandler) {
		return request(HttpMethod.GET, port, host, requestURI, responseHandler);
	}

	@Override
	public HttpClientRequest get(String host, String requestURI, Handler<HttpClientResponse> responseHandler) {
		if (httpClient == null) {
			responseHandler.handle(new ErrorHttpClientResponse(500, ""));
			return null;
		}
		final HttpClientRequest req = httpClient.get(host, requestURI, responseHandler);
		preConfigureRequest(responseHandler, req);
		return req;
	}

	@Override
	public HttpClientRequest get(String requestURI) {
		return request(HttpMethod.GET, requestURI);
	}

	@Override
	public HttpClientRequest getAbs(String absoluteURI) {
		return requestAbs(HttpMethod.GET, absoluteURI);
	}

	@Override
	public HttpClientRequest getAbs(String absoluteURI, Handler<HttpClientResponse> responseHandler) {
		return requestAbs(HttpMethod.GET, absoluteURI, responseHandler);
	}

	@Override
	public HttpClient getNow(RequestOptions options, Handler<HttpClientResponse> responseHandler) {
		get(options, responseHandler).end();
		return this;
	}

	@Override
	public HttpClient getNow(int port, String host, String requestURI, Handler<HttpClientResponse> responseHandler) {
		get(port, host, requestURI, responseHandler).end();
		return this;
	}

	@Override
	public HttpClient getNow(String host, String requestURI, Handler<HttpClientResponse> responseHandler) {
		get(host, requestURI, responseHandler);
		return this;
	}

	@Override
	public HttpClientRequest post(RequestOptions options) {
		return request(HttpMethod.POST, options);
	}

	@Override
	public HttpClientRequest post(int port, String host, String requestURI) {
		return request(HttpMethod.POST, port, host, requestURI);
	}

	@Override
	public HttpClientRequest post(String host, String requestURI) {
		return request(HttpMethod.POST, host, requestURI);
	}

	@Override
	public HttpClientRequest post(RequestOptions options, Handler<HttpClientResponse> responseHandler) {
		return request(HttpMethod.POST, options, responseHandler);
	}

	@Override
	public HttpClientRequest post(int port, String host, String requestURI, Handler<HttpClientResponse> responseHandler) {
		return request(HttpMethod.POST, port, host, requestURI, responseHandler);
	}

	@Override
	public HttpClientRequest post(String host, String requestURI, Handler<HttpClientResponse> responseHandler) {
		return request(HttpMethod.POST, host, requestURI, responseHandler);
	}

	@Override
	public HttpClientRequest post(String requestURI) {
		return request(HttpMethod.POST, requestURI);
	}

	@Override
	public HttpClientRequest postAbs(String absoluteURI) {
		return requestAbs(HttpMethod.POST, absoluteURI);
	}

	@Override
	public HttpClientRequest postAbs(String absoluteURI, Handler<HttpClientResponse> responseHandler) {
		return requestAbs(HttpMethod.POST, absoluteURI, responseHandler);
	}

	@Override
	public HttpClientRequest head(RequestOptions options) {
		return request(HttpMethod.HEAD, options);
	}

	@Override
	public HttpClientRequest head(int port, String host, String requestURI) {
		return request(HttpMethod.HEAD, port, host, requestURI);
	}

	@Override
	public HttpClientRequest head(String host, String requestURI) {
		return request(HttpMethod.HEAD, host, requestURI);
	}

	@Override
	public HttpClientRequest head(RequestOptions options, Handler<HttpClientResponse> responseHandler) {
		return request(HttpMethod.HEAD, options, responseHandler);
	}

	@Override
	public HttpClientRequest head(int port, String host, String requestURI, Handler<HttpClientResponse> responseHandler) {
		return request(HttpMethod.HEAD, port, host, requestURI, responseHandler);
	}

	@Override
	public HttpClientRequest head(String host, String requestURI, Handler<HttpClientResponse> responseHandler) {
		return request(HttpMethod.HEAD, host, requestURI, responseHandler);
	}

	@Override
	public HttpClientRequest head(String requestURI) {
		return request(HttpMethod.HEAD, requestURI);
	}

	@Override
	public HttpClientRequest headAbs(String absoluteURI) {
		return requestAbs(HttpMethod.HEAD, absoluteURI);
	}

	@Override
	public HttpClientRequest headAbs(String absoluteURI, Handler<HttpClientResponse> responseHandler) {
		return requestAbs(HttpMethod.HEAD, absoluteURI, responseHandler);
	}

	@Override
	public HttpClient headNow(RequestOptions options, Handler<HttpClientResponse> responseHandler) {
		head(options, responseHandler).end();
		return this;
	}

	@Override
	public HttpClient headNow(int port, String host, String requestURI, Handler<HttpClientResponse> responseHandler) {
		head(port, host, requestURI, responseHandler).end();
		return this;
	}

	@Override
	public HttpClient headNow(String host, String requestURI, Handler<HttpClientResponse> responseHandler) {
		head(host, requestURI, responseHandler).end();
		return this;
	}

	@Override
	public HttpClient headNow(String requestURI, Handler<HttpClientResponse> responseHandler) {
		head(requestURI, responseHandler).end();
		return this;
	}

	@Override
	public HttpClientRequest options(RequestOptions options) {
		return request(HttpMethod.OPTIONS, options);
	}

	@Override
	public HttpClientRequest options(int port, String host, String requestURI) {
		return request(HttpMethod.OPTIONS, port, host, requestURI);
	}

	@Override
	public HttpClientRequest options(String host, String requestURI) {
		return request(HttpMethod.OPTIONS, host, requestURI);
	}

	@Override
	public HttpClientRequest options(RequestOptions options, Handler<HttpClientResponse> responseHandler) {
		return request(HttpMethod.OPTIONS, options, responseHandler);
	}

	@Override
	public HttpClientRequest options(int port, String host, String requestURI, Handler<HttpClientResponse> responseHandler) {
		return request(HttpMethod.OPTIONS, port, host, requestURI, responseHandler);
	}

	@Override
	public HttpClientRequest options(String host, String requestURI, Handler<HttpClientResponse> responseHandler) {
		return request(HttpMethod.OPTIONS, host, requestURI, responseHandler);
	}

	@Override
	public HttpClientRequest options(String requestURI) {
		return request(HttpMethod.OPTIONS, requestURI);
	}

	@Override
	public HttpClientRequest optionsAbs(String absoluteURI) {
		return requestAbs(HttpMethod.OPTIONS, absoluteURI);
	}

	@Override
	public HttpClientRequest optionsAbs(String absoluteURI, Handler<HttpClientResponse> responseHandler) {
		return requestAbs(HttpMethod.OPTIONS, absoluteURI, responseHandler);
	}

	@Override
	public HttpClient optionsNow(RequestOptions options, Handler<HttpClientResponse> responseHandler) {
		options(options, responseHandler).end();
		return this;
	}

	@Override
	public HttpClient optionsNow(int port, String host, String requestURI, Handler<HttpClientResponse> responseHandler) {
		options(port, host, requestURI, responseHandler).end();
		return this;
	}

	@Override
	public HttpClient optionsNow(String host, String requestURI, Handler<HttpClientResponse> responseHandler) {
		options(host, requestURI, responseHandler).end();
		return this;
	}

	@Override
	public HttpClient optionsNow(String requestURI, Handler<HttpClientResponse> responseHandler) {
		options(requestURI, responseHandler).end();
		return this;
	}

	@Override
	public HttpClientRequest put(RequestOptions options) {
		return request(HttpMethod.PUT, options);
	}

	@Override
	public HttpClientRequest put(int port, String host, String requestURI) {
		return request(HttpMethod.PUT, port, host, requestURI);
	}

	@Override
	public HttpClientRequest put(String host, String requestURI) {
		return request(HttpMethod.PUT, host, requestURI);
	}

	@Override
	public HttpClientRequest put(RequestOptions options, Handler<HttpClientResponse> responseHandler) {
		return request(HttpMethod.PUT, options, responseHandler);
	}

	@Override
	public HttpClientRequest put(int port, String host, String requestURI, Handler<HttpClientResponse> responseHandler) {
		return request(HttpMethod.PUT, port, host, requestURI, responseHandler);
	}

	@Override
	public HttpClientRequest put(String host, String requestURI, Handler<HttpClientResponse> responseHandler) {
		return request(HttpMethod.PUT, host, requestURI, responseHandler);
	}

	@Override
	public HttpClientRequest put(String requestURI) {
		return request(HttpMethod.PUT, requestURI);
	}


	@Override
	public HttpClientRequest putAbs(String absoluteURI) {
		return requestAbs(HttpMethod.PUT, absoluteURI);
	}

	@Override
	public HttpClientRequest putAbs(String absoluteURI, Handler<HttpClientResponse> responseHandler) {
		return requestAbs(HttpMethod.PUT, absoluteURI, responseHandler);
	}

	@Override
	public HttpClientRequest delete(RequestOptions options) {
		return request(HttpMethod.DELETE, options);
	}

	@Override
	public HttpClientRequest delete(int port, String host, String requestURI) {
		return request(HttpMethod.DELETE, port, host, requestURI);
	}

	@Override
	public HttpClientRequest delete(String host, String requestURI) {
		return request(HttpMethod.DELETE, host, requestURI);
	}

	@Override
	public HttpClientRequest delete(RequestOptions options, Handler<HttpClientResponse> responseHandler) {
		return request(HttpMethod.DELETE, options, responseHandler);
	}

	@Override
	public HttpClientRequest delete(int port, String host, String requestURI, Handler<HttpClientResponse> responseHandler) {
		return request(HttpMethod.DELETE, port, host, requestURI, responseHandler);
	}

	@Override
	public HttpClientRequest delete(String host, String requestURI, Handler<HttpClientResponse> responseHandler) {
		return request(HttpMethod.DELETE, host, requestURI, responseHandler);
	}

	@Override
	public HttpClientRequest delete(String requestURI) {
		return request(HttpMethod.DELETE, requestURI);
	}


	@Override
	public HttpClientRequest deleteAbs(String absoluteURI) {
		return requestAbs(HttpMethod.DELETE, absoluteURI);
	}

	@Override
	public HttpClientRequest deleteAbs(String absoluteURI, Handler<HttpClientResponse> responseHandler) {
		return requestAbs(HttpMethod.DELETE, absoluteURI, responseHandler);
	}

	@Override
	public HttpClient redirectHandler(Function<HttpClientResponse, Future<HttpClientRequest>> handler) {
		httpClient.redirectHandler(handler);
		return this;
	}

	@Override
	public Function<HttpClientResponse, Future<HttpClientRequest>> redirectHandler() {
		return httpClient.redirectHandler();
	}

	@Override
	public boolean isMetricsEnabled() {
		return httpClient.isMetricsEnabled();
	}

	public class HalfOpenResult {

		private ResilientHttpClient r;

		HalfOpenResult(ResilientHttpClient resilientHttpClient) {
			this.r = resilientHttpClient;
		}

		public void fail() {
			r.openCircuit();
		}

		public void success() {
			r.closeCircuit();
		}
	}

	@Override
	public HttpClient getNow(String s, Handler<HttpClientResponse> handler) {
		if (httpClient == null) {
			handler.handle(new ErrorHttpClientResponse(500, ""));
			return this;
		}
		httpClient.getNow(s, handler);
		return this;
	}

	@Override
	public HttpClientRequest options(String s, Handler<HttpClientResponse> handler) {
		if (httpClient == null) {
			handler.handle(new ErrorHttpClientResponse(500, ""));
			return null;
		}
		final HttpClientRequest req = httpClient.options(s, handler);
		preConfigureRequest(handler, req);
		return req;
	}

	@Override
	public HttpClientRequest get(String s, Handler<HttpClientResponse> handler) {
		if (httpClient == null) {
			handler.handle(new ErrorHttpClientResponse(500, ""));
			return null;
		}
		final HttpClientRequest req = httpClient.get(s, handler);
		preConfigureRequest(handler, req);
		return req;
	}

	@Override
	public HttpClientRequest head(String s, Handler<HttpClientResponse> handler) {
		if (httpClient == null) {
			handler.handle(new ErrorHttpClientResponse(500, ""));
			return null;
		}
		final HttpClientRequest req = httpClient.head(s, handler);
		preConfigureRequest(handler, req);
		return req;
	}

	@Override
	public HttpClientRequest post(String s, Handler<HttpClientResponse> handler) {
		if (httpClient == null) {
			handler.handle(new ErrorHttpClientResponse(500, ""));
			return null;
		}
		final HttpClientRequest req = httpClient.post(s, handler);
		preConfigureRequest(handler, req);
		return req;
	}

	@Override
	public HttpClientRequest put(String s, Handler<HttpClientResponse> handler) {
		if (httpClient == null) {
			handler.handle(new ErrorHttpClientResponse(500, ""));
			return null;
		}
		final HttpClientRequest req = httpClient.put(s, handler);
		preConfigureRequest(handler, req);
		return req;
	}

	@Override
	public HttpClientRequest delete(String s, Handler<HttpClientResponse> handler) {
		if (httpClient == null) {
			handler.handle(new ErrorHttpClientResponse(500, ""));
			return null;
		}
		final HttpClientRequest req = httpClient.delete(s, handler);
		preConfigureRequest(handler, req);
		return req;
	}

	@Override
	public void close() {
		httpClient.close();
	}

	public HttpClient setHalfOpenHandler(Handler<HalfOpenResult> halfOpenHandler) {
		this.halfOpenHandler = halfOpenHandler;
		return this;
	}

	private void preConfigureRequest(final Handler<HttpClientResponse> handler, HttpClientRequest req) {
		req.exceptionHandler(new Handler<Throwable>() {
			@Override
			public void handle(Throwable throwable) {
				log.error("SwiftHttpClient : request error", throwable);
				if (errorsCount.incrementAndGet() > threshold) {
					openCircuit();
				}
				if (handler != null) {
					handler.handle(new ErrorHttpClientResponse(500,
							((throwable != null && throwable.getMessage() != null) ? throwable.getMessage() : "")));
				}
			}
		});
		req.setTimeout(timeout);
	}

	private void openCircuit() {
		log.info("SwiftHttpClient : open circuit");
		if (closedCircuit.getAndSet(false) && httpClient != null) {
			httpClient.close();
			httpClient = null;
		}
		errorsCount.set(0);
		vertx.setTimer(openDelay, new Handler<Long>() {
			@Override
			public void handle(Long aLong) {
				reconfigure();
			}
		});
	}

	private void reconfigure() {
		final int port = (uri.getPort() > 0) ? uri.getPort() : ("https".equals(uri.getScheme()) ? 443 : 80);
		HttpClientOptions options = new HttpClientOptions()
				.setDefaultHost(uri.getHost())
				.setDefaultPort(port)
				.setMaxPoolSize(16)
				.setSsl("https".equals(uri.getScheme()))
				.setKeepAlive(keepAlive)
				.setConnectTimeout(timeout);
		this.httpClient = vertx.createHttpClient(options);

//		this.httpClient.exceptionHandler(new Handler<Throwable>() {
//			@Override
//			public void handle(Throwable throwable) {
//				log.error("SwiftHttpClient : global error", throwable);
//				if (errorsCount.incrementAndGet() > threshold) {
//					openCircuit();
//				}
//			}
//		});
		log.info("SwiftHttpClient : half-close circuit");
		if (halfOpenHandler != null) {
			halfOpenHandler.handle(new HalfOpenResult(this));
		}
	}

	private void closeCircuit() {
		log.info("SwiftHttpClient : close circuit");
		closedCircuit.set(true);
	}

}
