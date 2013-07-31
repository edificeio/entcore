package edu.one.core.history;

import static edu.one.core.infra.http.Renders.*;

import edu.one.core.infra.Server;
import edu.one.core.infra.http.Renders;

import org.vertx.java.core.Handler;
import org.vertx.java.core.buffer.Buffer;
import org.vertx.java.core.http.HttpServerRequest;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;

public class History extends Server {

	@Override
	public void start() {
		super.start();
		final Renders render = new Renders(container);

		rm.get("/admin", new Handler<HttpServerRequest>() {
			@Override
			public void handle(HttpServerRequest request) {
				JsonObject traceFiles = new JsonObject().putArray("traceFiles", new JsonArray());
				for (String file : vertx.fileSystem().readDirSync(config.getString("log-path"))) {
					if (!file.endsWith(".trace")) { continue; }
					file = file.replace(config.getString("log-path"), "").replace(".trace", "");
					traceFiles.getArray("traceFiles").addObject(new JsonObject().putString("name", file));
				}
				render.renderView(request, traceFiles);
			}
		});

		rm.get("/admin/logs", new Handler<HttpServerRequest>() {
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
