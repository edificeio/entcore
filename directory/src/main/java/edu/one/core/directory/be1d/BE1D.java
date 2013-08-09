package edu.one.core.directory.be1d;

import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.vertx.java.core.Handler;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.platform.Container;

import edu.one.core.infra.Neo;

public class BE1D {

	private final Vertx vertx;
	private final Container container;
	private final String porteurFolder;
	private final Neo neo;

	public BE1D(Vertx vertx, Container container, String porteurFolder) {
		this.vertx = vertx;
		this.container = container;
		this.porteurFolder = porteurFolder;
		this.neo = new Neo(vertx.eventBus(), container.logger());
	}

	public void importPorteur(final Handler<JsonArray> handler) {
		final AtomicInteger remaining = new AtomicInteger(0);
		neo.send(
				"START n=node:node_auto_index(type='ETABEDUCNAT') " +
				"RETURN n.ENTStructureNomCourant as name", new Handler<Message<JsonObject>>() {

			@Override
			public void handle(Message<JsonObject> res) {
				if ("ok".equals(res.body().getString("status"))) {
					JsonObject j = res.body().getObject("result");
					List<String> existingSchools = new ArrayList<>();
					for (String attr: j.getFieldNames()) {
						existingSchools.add(attr);
					}
					File [] folders = new File(porteurFolder).listFiles(new  FileFilter() {
						@Override
						public boolean accept(File f) {
							return f.isDirectory();
						}
					});
					final JsonArray imports = new JsonArray();
					final Handler<JsonObject> h = new Handler<JsonObject>() {

						@Override
						public void handle(JsonObject json) {
							imports.addObject(json);
							if (remaining.decrementAndGet() == 0) {
								handler.handle(imports);
							}
						}
					};
					for (File f :folders) {
						if (!existingSchools.contains(f.getName())) {
							remaining.incrementAndGet();
							String schoolFolder = porteurFolder + File.separator + f.getName();
							new BE1DImporter(vertx, container, schoolFolder)
							.importSchool(f.getName(), h);
						}
					}
				} else {
					handler.handle(new JsonArray().addObject(res.body()));
				}
			}
		});
	}

}
