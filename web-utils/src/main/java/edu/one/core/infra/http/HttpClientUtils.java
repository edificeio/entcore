package edu.one.core.infra.http;

import org.vertx.java.core.Handler;
import org.vertx.java.core.MultiMap;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.VoidHandler;
import org.vertx.java.core.buffer.Buffer;
import org.vertx.java.core.http.HttpClient;
import org.vertx.java.core.http.HttpClientRequest;
import org.vertx.java.core.http.HttpClientResponse;
import org.vertx.java.core.http.HttpServerRequest;
import org.vertx.java.core.json.JsonObject;

public class HttpClientUtils {

	public static void sendFile(Vertx vertx, String uri, int port, String content,
			MultiMap headers, String filename,
			String contentType, Handler<HttpClientResponse> handler) {
		HttpClientRequest req = vertx.createHttpClient().setPort(port).post(uri, handler);

		final String boundary = "dLV9Wyq26L_-JQxk6ferf-RT153LhOO";
		Buffer buffer = new Buffer();
		final String body = "--" + boundary + "\r\n" +
				"Content-Disposition: form-data; name=\"file\"; filename=\""+ filename +"\"\r\n" +
				"Content-Type: " + contentType + "\r\n" +
				"\r\n" +
				content + "\r\n" +
				"--" + boundary + "--\r\n";

		buffer.appendString(body);
		req.headers().add(headers);
		req.headers().set("content-length", String.valueOf(buffer.length()));
		req.headers().set("content-type", "multipart/form-data; boundary=" + boundary);
		req.write(buffer).end();
	}

	public static void proxy(final HttpServerRequest req, HttpClient client) {
		proxy(req, client, null, null, null);
	}

	public static void proxy(final HttpServerRequest req, HttpClient client, String prefix) {
		proxy(req, client, prefix, null, null);
	}

	public static void proxy(final HttpServerRequest req, HttpClient client,
			String prefix, String replacement) {
		proxy(req, client, prefix, replacement, null);
	}

	public static void proxy(final HttpServerRequest req, HttpClient client,
			String prefix, String replacement, final JsonObject defaultResult) {
		String uri = req.uri();
		if (prefix != null && !prefix.trim().isEmpty()) {
			if (replacement != null && !replacement.trim().isEmpty()) {
				uri = uri.replaceFirst(prefix, replacement);
			} else {
				uri = uri.replaceFirst(req.path(), prefix + req.path());
			}
		}
		final HttpClientRequest cReq = client.request(req.method(), uri,
				new Handler<HttpClientResponse>() {
			public void handle(HttpClientResponse cRes) {
				if (defaultResult != null && defaultResult.getString("content") != null &&
						(cRes.statusCode() < 200 || (cRes.statusCode() >= 300 &&
						cRes.statusCode() != 304))) {
					if (defaultResult.getObject("headers") != null) {
						for (String header: defaultResult.getObject("headers").getFieldNames()) {
							req.response().headers().add(header,
									defaultResult.getObject("headers").getString(header));
						}
					}
					if ("file".equals(defaultResult.getString("type"))) {
						req.response().sendFile(defaultResult.getString("content"));
					} else {
						req.response().end(defaultResult.getString("content"));
					}
				} else {
					req.response().setStatusCode(cRes.statusCode());
					req.response().headers().set(cRes.headers());
					req.response().setChunked(true);
					cRes.dataHandler(new Handler<Buffer>() {
						public void handle(Buffer data) {
							req.response().write(data);
						}
					});
					cRes.endHandler(new VoidHandler() {
						public void handle() {
							req.response().end();
						}
					});
				}
			}
		});
		cReq.headers().set(req.headers());
		cReq.putHeader("Host", client.getHost());
		cReq.setChunked(true);
		req.dataHandler(new Handler<Buffer>() {
			public void handle(Buffer data) {
				cReq.write(data);
			}
		});
		req.endHandler(new VoidHandler() {
			public void handle() {
				cReq.end();
			}
		});
	}

}
