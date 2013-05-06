package edu.one.core.directory.test;

import org.junit.Test;
import org.vertx.java.core.AsyncResult;
import org.vertx.java.core.Handler;
import org.vertx.java.core.buffer.Buffer;
import org.vertx.java.core.http.HttpClient;
import org.vertx.java.core.http.HttpClientResponse;
import org.vertx.java.core.json.JsonObject;
import org.vertx.testtools.TestVerticle;
import static org.vertx.testtools.VertxAssert.*;

public class DirectoryTest extends TestVerticle {

	@Test
	public void UserCreationTest() throws Exception{
		final HttpClient client = vertx.createHttpClient().setPort(8003);
		System.out.println(System.getProperty("user.dir"));
		container.deployModule("edu.one.core~directory~0.1.0-SNAPSHOT", new Handler<AsyncResult<String>>() {
			@Override
			public void handle(AsyncResult<String> event) {
				if (event.succeeded()){
					client.get("/api/create-user?ENTPersonNom=Tom&ENTPersonPrenom=Jones"
							+ "&ENTPersonDateNaissance=11%2F12%2F1989", new Handler<HttpClientResponse>() {
						@Override
						public void handle(HttpClientResponse resp) {
							resp.bodyHandler(new Handler<Buffer>(){
								@Override
								public void handle(Buffer data) {
									JsonObject result = new JsonObject(data.toString());
									assertFalse(result.getString("result").equals("error"));
									testComplete();
								}
							});
							System.out.println("Status : " + resp.statusMessage());
							testComplete();
						}
					});
				} else {
					System.out.println("Failed to load module : " + event.cause());
					testComplete();
				}
			}
		});
	}


	@Test
	public void SchoolCreationTest() throws Exception{
		final HttpClient client = vertx.createHttpClient().setPort(8003);
		System.out.println(System.getProperty("user.dir"));
		container.deployModule("edu.one.core~directory~0.1.0-SNAPSHOT", new Handler<AsyncResult<String>>() {
			@Override
			public void handle(AsyncResult<String> event) {
				if (event.succeeded()){
					client.get("/api/create-school?ENTSchoolId=1234&ENTSchoolFoo=bar"
							, new Handler<HttpClientResponse>() {
						@Override
						public void handle(HttpClientResponse resp) {
							resp.bodyHandler(new Handler<Buffer>(){
								@Override
								public void handle(Buffer data) {
									JsonObject result = new JsonObject(data.toString());
									assertTrue(result.getString("result").equals("error"));
									assertFalse(result.getBoolean("ENTPersonDateNaissance"));
									testComplete();
								}
							});
							System.out.println("Status  : " + resp.statusMessage());
							testComplete();
						}
					});
				} else {
					System.out.println("Failed to load module : " + event.cause());
					testComplete();
				}
			}
		});
	}


	@Test
	public void EmptyExportTest() throws Exception{
		final HttpClient client = vertx.createHttpClient().setPort(8003);
		container.deployModule("edu.one.core~directory~0.1.0-SNAPSHOT", new Handler<AsyncResult<String>>() {
			@Override
			public void handle(AsyncResult<String> event) {
				if (event.succeeded()){
					client.get("/api/export?id=4400000001", new Handler<HttpClientResponse>() {
						@Override
						public void handle(HttpClientResponse resp) {
							resp.bodyHandler(new Handler<Buffer>(){
								@Override
								public void handle(Buffer data) {
									JsonObject result = new JsonObject(data.toString());
									assertTrue(result.getField("result").equals(""));
									testComplete();
								}
							});
							System.out.println("Status : " + resp.statusMessage());
							testComplete();
						}
					});
				} else {
					System.out.println("Failed to load module : " + event.cause());
					testComplete();
				}
			}
		});
	}


	@Test
	public void ExportErrorTest() throws Exception{
		final HttpClient client = vertx.createHttpClient().setPort(8003);
		container.deployModule("edu.one.core~directory~0.1.0-SNAPSHOT", new Handler<AsyncResult<String>>() {
			@Override
			public void handle(AsyncResult<String> event) {
				if (event.succeeded()){
					client.get("/api/export?id=442", new Handler<HttpClientResponse>() {
						@Override
						public void handle(HttpClientResponse resp) {
							resp.bodyHandler(new Handler<Buffer>(){
								@Override
								public void handle(Buffer data) {
									JsonObject result = new JsonObject(data.toString());
									assertTrue(result.getField("result").equals(""));
									testComplete();
								}
							});
							System.out.println("Status : " + resp.statusMessage());
							testComplete();
						}
					});
				} else {
					System.out.println("Failed to load module : " + event.cause());
					testComplete();
				}
			}
		});
	}
}
