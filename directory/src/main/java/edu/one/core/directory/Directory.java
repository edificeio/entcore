package edu.one.core.directory;

import edu.one.core.infra.Controller;
import java.util.Map;
import org.vertx.java.core.Handler;
import org.vertx.java.core.http.HttpServerRequest;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;

public class Directory extends Controller {

	@Override
	public void start() throws Exception {
		super.start();
		final JsonObject dataMock = new JsonObject(vertx.fileSystem().readFileSync("directory-data-mock.json").toString());
		
		rm.get("/directory/admin", new Handler<HttpServerRequest>() {
			@Override
			public void handle(HttpServerRequest request) {
				renderView(request, new JsonObject());
			}
		});
		
		rm.get("/directory/api/ecole", new Handler<HttpServerRequest>() {
			@Override
			public void handle(HttpServerRequest request) {
				renderJson(request, dataMock.getObject("ecole"));
				
			}
		});
		
		rm.get("/directory/api/classes", new Handler<HttpServerRequest>() {
			@Override
			public void handle(HttpServerRequest request) {
				request.response.putHeader("content-type", "text/json");
				request.response.end(dataMock.getArray("classes").encode());
				//renderJson(request.response, dataMock.getObject("classes"));
				
			}
		});
		
		rm.get("/directory/api/personnes", new Handler<HttpServerRequest>() {
			@Override
			public void handle(HttpServerRequest request) {
				request.response.putHeader("content-type", "text/json");
				request.response.end(dataMock.getArray("personnes").encode());
				//renderJson(request.response, dataMock.getObject("personnes"));
				
			}
		});
		
		rm.get("/directory/api/details", new Handler<HttpServerRequest>() {
			@Override
			public void handle(HttpServerRequest request) {
				JsonArray people = dataMock.getArray("personnes");
				for (Object object : people) {
					JsonObject jo = (JsonObject)object;
					if (jo.getInteger("id").equals(new Integer(request.params().get("id")))){
						renderJson(request, jo);
					}
				}
			}
		});
		
		rm.post("/directory/api/edit", new Handler<HttpServerRequest>() {
			@Override
			public void handle(final HttpServerRequest request) {
				bodyToParams(request, new Handler<Map<String, String>>() {
					@Override
					public void handle(Map<String, String> params) {
						JsonObject jo = new JsonObject().putString("message", "success");
						renderJson(request, jo);
					}
				});
				
			}
		});
	}

}
