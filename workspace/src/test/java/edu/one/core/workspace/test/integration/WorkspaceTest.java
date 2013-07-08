package edu.one.core.workspace.test.integration;

import static org.vertx.testtools.VertxAssert.assertEquals;
import static org.vertx.testtools.VertxAssert.assertTrue;
import static org.vertx.testtools.VertxAssert.testComplete;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.concurrent.ExecutionException;

import org.apache.commons.codec.digest.DigestUtils;
import org.junit.Test;
import org.vertx.java.core.AsyncResult;
import org.vertx.java.core.AsyncResultHandler;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;
import org.vertx.testtools.TestVerticle;

import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.AsyncHttpClientConfig;
import com.ning.http.client.Request;
import com.ning.http.client.RequestBuilder;
import com.ning.http.client.Response;
import com.ning.http.multipart.FilePart;


public class WorkspaceTest extends TestVerticle {

	private static final String RESOURCE_FILE = "/home/dboissin/Downloads/wedding.jpg";

	@Override
	public void start() {
		JsonObject mongoConfig = new JsonObject();
		mongoConfig.putString("address", "wse.mongodb.persistor");
		mongoConfig.putString("db_name", System.getProperty("vertx.mongo.database", "test_db"));
		mongoConfig.putString("host", System.getProperty("vertx.mongo.host", "localhost"));
		mongoConfig.putNumber("port", Integer.valueOf(System.getProperty("vertx.mongo.port", "27017")));
		JsonObject config = new JsonObject();
		config.putBoolean("auto-redeploy", false);
		config.putNumber("port", 8011);
		config.putObject("mongodb-config", mongoConfig);
		config.putString("files-repository", "/tmp");

		container.deployModule("edu.one.core~workspace~0.1.0-SNAPSHOT", config, 1, new AsyncResultHandler<String>() {
			public void handle(AsyncResult<String> ar) {
				if (ar.succeeded()) {
					WorkspaceTest.super.start();
				} else {
					ar.cause().printStackTrace();
				}
			}
		});
	}

	private Response sendDocument(File file, String method, String url) throws FileNotFoundException,
			InterruptedException, ExecutionException, IOException {
		AsyncHttpClientConfig.Builder bc = new AsyncHttpClientConfig.Builder();
		bc.setFollowRedirects(true);
		AsyncHttpClient c = new AsyncHttpClient(bc.build());
		RequestBuilder builder = new RequestBuilder(method);
		builder.setUrl(url);
		builder.addBodyPart(new FilePart("file", file));
		Request r = builder.build();
		Response res = c.executeRequest(r).get();
		c.close();
		return res;
	}

	private Response sendDocument(File file) throws FileNotFoundException,
			InterruptedException, ExecutionException, IOException {
		return sendDocument(file, "POST", "http://localhost:8011/document?name=Panda");
	}

	@Test
	public void testPostDocument() throws Exception {
		Response res = sendDocument(new File(RESOURCE_FILE));
		container.logger().info(res.getResponseBody());

		assertEquals(201, res.getStatusCode());
		assertEquals("ok", new JsonObject(res.getResponseBody()).getString("status"));
		testComplete();
	}

	@Test
	public void testPutDocument() throws Exception {
		Response res = sendDocument(new File(RESOURCE_FILE));
		JsonObject json = new JsonObject(res.getResponseBody());

		Response r = sendDocument(new File(RESOURCE_FILE), "PUT",
				"http://localhost:8011/document/" + json.getString("_id") + "?name=blip");
		assertEquals(200, r.getStatusCode());
		assertEquals("ok", new JsonObject(r.getResponseBody()).getString("status"));
		assertEquals(1, new JsonObject(r.getResponseBody()).getNumber("number"));
		testComplete();
	}

	@Test
	public void testGetDocument() throws Exception {
		File file = new File(RESOURCE_FILE);
		String md5 = DigestUtils.md5Hex(new FileInputStream(file));
		Response res = sendDocument(file);
		JsonObject result = new JsonObject(res.getResponseBody());

		AsyncHttpClient c = new AsyncHttpClient();
		Response r = c.prepareGet("http://localhost:8011/document/" + result.getString("_id"))
				.execute().get();
		c.close();

		assertEquals(200, r.getStatusCode());
		assertEquals(md5, DigestUtils.md5Hex(r.getResponseBodyAsStream()));
		testComplete();
	}

	@Test
	public void testDeleteDocument() throws Exception {
		Response res = sendDocument(new File(RESOURCE_FILE));
		JsonObject result = new JsonObject(res.getResponseBody());

		AsyncHttpClient c = new AsyncHttpClient();
		Response r = c.prepareDelete("http://localhost:8011/document/" + result.getString("_id"))
				.execute().get();
		c.close();

		assertEquals(204, r.getStatusCode());
		testComplete();
	}

	@Test
	public void testListDocuments() throws Exception {
		sendDocument(new File(RESOURCE_FILE));
		sendDocument(new File(RESOURCE_FILE));
		sendDocument(new File(RESOURCE_FILE));

		AsyncHttpClient c = new AsyncHttpClient();
		Response r = c.prepareGet("http://localhost:8011/documents").execute().get();
		c.close();

		JsonArray list = new JsonArray(r.getResponseBody());

		assertEquals(200, r.getStatusCode());
		assertTrue(list.size() >= 3);
		testComplete();
	}

}
