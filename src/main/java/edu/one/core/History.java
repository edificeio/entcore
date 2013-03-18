package edu.one.core;

import edu.one.core.infra.Controller;
import org.vertx.java.core.Handler;
import org.vertx.java.core.buffer.Buffer;
import org.vertx.java.core.http.HttpServerRequest;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;

public class History extends Controller {

	@Override
	public void start() throws Exception {
		super.start();

		rm.get("/history/admin", new Handler<HttpServerRequest>() {
			@Override
			public void handle(HttpServerRequest request) {
				renderView(request, new JsonObject());
			}
		});

		rm.get("/history/admin/logs", new Handler<HttpServerRequest>() {
			@Override
			public void handle(final HttpServerRequest request) {
				try {
					String tracesFile = config.getString("log-path") + request.params().get("app") + ".trace";
					renderJson(request, tracesToJson(tracesFile));
				} catch (Exception ex) {
					// TODO : manage end-user error message (i18n , human compatible message ...)
					renderError(request);
				}
			}
		});

	}

	// TODO : Try to writer a logFormatter in Trace Module that avoid this bloat operations
	private JsonObject tracesToJson(String logFileName) throws Exception {
		Buffer b = vertx.fileSystem().readFileSync(logFileName);
		String traces = b.toString().trim();
		String tracesArray = "[" + traces.substring(0, traces.length() -1) + "]";
		JsonObject jo = new JsonObject().putArray("records", new JsonArray(tracesArray));
		return jo;
	}

}
