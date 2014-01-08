package edu.one.core.sync;

import static edu.one.core.infra.http.Renders.*;

import edu.one.core.infra.Server;
import edu.one.core.infra.http.Renders;
import edu.one.core.sync.aaf.SyncManager;

import org.vertx.java.core.Handler;
import org.vertx.java.core.http.HttpServerRequest;
import org.vertx.java.core.json.JsonObject;

public class Sync extends Server {
	private SyncManager syncMgr;

	@Override
	public void start() {
		super.start();
		final Renders render = new Renders(vertx, container);
		syncMgr = new SyncManager(trace, vertx, container);

		rm.get("/admin", new Handler<HttpServerRequest>() {
			@Override
			public void handle(HttpServerRequest request) {
				render.renderView(request);
			}
		});

		rm.get("/admin/aaf/test", new Handler<HttpServerRequest>() {
			@Override
			public void handle(HttpServerRequest request) {
				try {
					long startTest = System.currentTimeMillis();
					int[] crTest = syncMgr.syncAaf(config.getString("input-files-folder"));
					long endTest = System.currentTimeMillis();

					JsonObject jo = new JsonObject().putObject("result",
						new JsonObject()
							.putString("temps", (endTest - startTest) + " ms")
							.putNumber("operations", crTest[0])
							.putNumber("rejets", crTest[1])
					);
					renderJson(request, jo);
				} catch (Exception ex) {
					trace.error(ex.toString());
					renderError(request);
				}
			}
		});
	}
}