package edu.one.core.myAccount;

import edu.one.core.infra.Controller;
import org.vertx.java.core.Handler;
import org.vertx.java.core.buffer.Buffer;
import org.vertx.java.core.http.HttpClient;
import org.vertx.java.core.http.HttpClientRequest;
import org.vertx.java.core.http.HttpClientResponse;
import org.vertx.java.core.http.HttpServerRequest;
import org.vertx.java.core.json.JsonObject;

public class MyAccount extends Controller {

	JsonObject users;

	@Override
	public void start() {
		super.start();
		final JsonObject dataMock = 
				new JsonObject(vertx.fileSystem().readFileSync("myAccount-data-mock.json").toString());

		rm.get("/index", new Handler<HttpServerRequest>() {
			@Override
			public void handle(HttpServerRequest request) {
				renderView(request, dataMock);
			}
		});

		rm.get("/load", new Handler<HttpServerRequest>() {
			@Override
			public void handle(HttpServerRequest request) {
				renderJson(request, dataMock);
			}
		});

		rm.get("/classe", new Handler<HttpServerRequest>() {
			@Override
			public void handle(final HttpServerRequest request) {
				getDirectoryData("/api/personnes?id=4400000002$ORDINAIRE$CM2%20de%20Mme%20Rousseau");
				renderView(request, users);
			}
		});
		
		rm.get("/person", new Handler<HttpServerRequest>() {
			@Override
			public void handle(final HttpServerRequest request) {
				HttpClient client = vertx.createHttpClient().setPort(8003);
				HttpClientRequest req = client.get("/api/details?id=" + request.params().get("id"), new Handler<HttpClientResponse>() {
					public void handle(HttpClientResponse resp) {
						resp.bodyHandler(new Handler<Buffer>() {
							public void handle(Buffer data) {
								renderJson(request, new JsonObject(data.toString()));
							}
						});
					}
				});
				req.end();
			}
		});

		rm.get("/load-class", new Handler<HttpServerRequest>() {
			@Override
			public void handle(HttpServerRequest request) {
				renderJson(request, users);
			}
		});
	}

	private void getDirectoryData(String apiUrl){
		HttpClient client = vertx.createHttpClient().setPort(8003);
		HttpClientRequest req = client.get(apiUrl, new Handler<HttpClientResponse>() {
			public void handle(HttpClientResponse resp) {
				resp.bodyHandler(new Handler<Buffer>() {
					public void handle(Buffer data) {
						users = new JsonObject(data.toString());
					}
				});
			}
		});
		req.end();
	}
}