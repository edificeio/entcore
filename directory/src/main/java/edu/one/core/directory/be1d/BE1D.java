package edu.one.core.directory.be1d;

import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.List;

import org.vertx.java.core.Handler;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.platform.Container;

import edu.one.core.common.neo4j.Neo;
import edu.one.core.infra.Server;

public class BE1D {

	private final Vertx vertx;
	private final Container container;
	private final String porteurFolder;
	private final Neo neo;

	public BE1D(Vertx vertx, Container container, String porteurFolder) {
		this.vertx = vertx;
		this.container = container;
		this.porteurFolder = porteurFolder;
		this.neo = new Neo(Server.getEventBus(vertx), container.logger());
	}

	public void importPorteur(final Handler<JsonArray> handler) {
		neo.send(
				"MATCH (n:School) " +
				"RETURN n.name as name", new Handler<Message<JsonObject>>() {

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
					final List<Handler<JsonObject>> handlers = new ArrayList<>();
					handlers.add(new Handler<JsonObject>() {

						@Override
						public void handle(JsonObject json) {
							imports.addObject(json);
							handler.handle(imports);
						}
					});
					for (File folder :folders) {
						final File f = folder;
						if (!existingSchools.contains(f.getName())) {
							handlers.add(new Handler<JsonObject>() {

								@Override
								public void handle(JsonObject event) {
									handlers.remove(handlers.size() - 1);
									if (event != null) {
										imports.addObject(event);
									}
									String schoolFolder = porteurFolder + File.separator + f.getName();
									String schoolName = f.getName();
									String UAI = "";
									if (f.getName().contains("|")) {
										int idx = f.getName().lastIndexOf("|");
										String tUAI = f.getName().substring(idx + 1);
										if (tUAI != null && tUAI.matches("[0-9]{7}[A-Z]")) {
											UAI = tUAI;
											schoolName = schoolName.substring(0, idx);
										}
									}
									new BE1DImporter(vertx, container, schoolFolder)
									.importSchool(schoolName, UAI, handlers.get(handlers.size() - 1));
								}
							});
						}
					}
					handlers.get(handlers.size() - 1).handle(null);
				} else {
					handler.handle(new JsonArray().addObject(res.body()));
				}
			}
		});
	}

}
