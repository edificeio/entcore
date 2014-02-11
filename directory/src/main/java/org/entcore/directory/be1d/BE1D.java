package org.entcore.directory.be1d;

import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.vertx.java.core.Handler;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.platform.Container;

import org.entcore.common.neo4j.Neo;
import fr.wseduc.webutils.Server;

public class BE1D {

	private final Vertx vertx;
	private final Container container;
	private final String porteurFolder;
	private final Neo neo;
	private static final Set<String> existingSchools = new HashSet<>();

	public BE1D(Vertx vertx, Container container, String porteurFolder) {
		this.vertx = vertx;
		this.container = container;
		this.porteurFolder = porteurFolder;
		this.neo = new Neo(Server.getEventBus(vertx), container.logger());
	}

	public void importPorteur(final Handler<JsonArray> handler) {
		neo.execute(
				"MATCH (n:School) " +
				"RETURN n.name as name", new JsonObject(), new Handler<Message<JsonObject>>() {

			@Override
			public void handle(Message<JsonObject> res) {
				if ("ok".equals(res.body().getString("status"))) {
					for (Object o: res.body().getArray("result")) {
						if (!(o instanceof JsonObject)) continue;
						JsonObject j = (JsonObject) o;
						existingSchools.add(j.getString("name"));
					}
					File [] folders = new File(porteurFolder).listFiles(new  FileFilter() {
						@Override
						public boolean accept(File f) {
							return f.isDirectory();
						}
					});
					List<String> fName = new ArrayList<>();
					for (File folder: folders) {
						fName.add(folder.getName());
					}
					if (fName.isEmpty() || existingSchools.containsAll(fName)) {
						handler.handle(new JsonArray());
						return;
					}
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
							existingSchools.add(f.getName());
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
									String separator = container.config().getString("uai-separator","|");
									if (f.getName().contains(separator)) {
										int idx = f.getName().lastIndexOf(separator);
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
