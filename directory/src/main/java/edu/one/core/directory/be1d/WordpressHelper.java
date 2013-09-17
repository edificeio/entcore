/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.one.core.directory.be1d;

import java.util.HashMap;
import java.util.Map;

import org.vertx.java.core.Handler;
import org.vertx.java.core.eventbus.EventBus;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;

/**
 *
 * @author bperez
 */
public class WordpressHelper {
	private EventBus eb;
	private Map<String, JsonObject> schools;
	private Map<String, JsonObject> groups;
	private String SCHOOL_ATTR = "ecole";
	private String CLASS_ATTR = "classe";
	private final String school;

	public WordpressHelper(EventBus eb, String school) {
		this.eb = eb;
		this.school = school;
		schools = new HashMap<>();
		groups = new HashMap<>();
	}

	public void queryToEntity(JsonObject json) {
		if (json != null && json.getObject("params") != null) {
			addEntity(json.getObject("params"));
		}
	}

	public void addEntity(JsonObject entity) {
		// TODO : contrôles existence, not null, etc.
		switch (entity.getString("type", "NOP")) {
			case "ETABEDUCNAT" :
				entity.putString("ENTStructureNomCourant",
						school + entity.getString("ENTStructureNomCourant"));
				schools.put(entity.getString("id"), entity);
				break;
			case "CLASSE" :
				addGroup(entity);
				break;
			case "ENSEIGNANT":
			case "ELEVE":
			case "PERSRELELEVE":
			case "PERSEDUCNAT" :
				break;
			default :
				break;
		}
	}

	private void addGroup(JsonObject json) {
		if (json.getString("ENTGroupeNom") != null) {
			json.putString("ENTGroupeNom", json.getString("ENTGroupeNom"));
		} else if (json.getString("name") != null) {
			json.putString("ENTGroupeNom", school + json.getString("name"));
		}
		groups.put(json.getString("id"), json.putString(SCHOOL_ATTR, school));
	}

	private void addPersonClass(JsonObject json) {
		if (json.getString("ENTPersonClasses") != null && !json.getString("ENTPersonClasses").contains("|")) {
			json.putString(CLASS_ATTR, school + json.getString("ENTPersonClasses"));
		} else if (json.getString("ENTPersonClasses") != null) {
			String [] classes = json.getString("ENTPersonClasses").split("\\|");
			StringBuilder sb = new StringBuilder();
			for (int i = 0; i < classes.length; i++) {
				sb.append("|" + school + classes[i]);
			}
			json.putString(CLASS_ATTR, sb.substring(1).toString());
		}
	}

	public void send() throws InterruptedException {
		for (Map.Entry<String, JsonObject> entry : schools.entrySet()) {
			sendWP(entry.getValue());
		}
		Thread.sleep(5000);
		for (Map.Entry<String, JsonObject> entry : groups.entrySet()) {
			sendWP(entry.getValue());
		}
		Thread.sleep(5000);
		usersFromNeo();

//		final AtomicInteger sNb = new AtomicInteger(schools.size());
//		final AtomicInteger gNb = new AtomicInteger(groups.size());
//		for (Map.Entry<String, JsonObject> entry : schools.entrySet()) {
//			sendWP(entry.getValue(), new Handler<Message<JsonObject>>() {
//
//				@Override
//				public void handle(Message<JsonObject> event) {
//					System.out.println("handle school");
//					if (sNb.getAndDecrement() <= 1) {
//						for (Map.Entry<String, JsonObject> entry : groups.entrySet()) {
//							sendWP(entry.getValue(), new Handler<Message<JsonObject>>() {
//
//								@Override
//								public void handle(Message<JsonObject> event) {
//									System.out.println("handle group");
//									if (gNb.getAndDecrement() <= 1) {
//										for (Map.Entry<String, JsonObject> entry : groups.entrySet()) {
//											sendWP(entry.getValue());
//										}
//									}
//								}
//							});
//						}
//					}
//				}
//			});
//		}
	}

	private void usersFromNeo() {
		String query =
				"START n=node:node_auto_index(id={schoolId}), u=node:node_auto_index({types}) " +
				"MATCH n<-[:DEPENDS]-gpe<-[:APPARTIENT]-u " +
				"RETURN distinct u.id as id, u.type as type, u.ENTPersonLogin as ENTPersonLogin, " +
				"u.activationCode as activationCode, u.ENTPersonClasses as ENTPersonClasses, " +
				"u.ENTPersonNomAffichage as ENTPersonNomAffichage";
		JsonObject params = new JsonObject()
		.putString("schoolId", school)
		.putString("types", "type:ELEVE OR type:ENSEIGNANT OR type:PERSEDUCNAT OR type:PERSRELELEVE");
		JsonObject jo = new JsonObject();
		jo.putString("action", "execute");
		jo.putString("query", query);
		jo.putObject("params", params);
		eb.send("wse.neo4j.persistor", jo, new Handler<Message<JsonObject>>() {

			@Override
			public void handle(Message<JsonObject> res) {
				if ("ok".equals(res.body().getString("status"))) {
					for (String attr : res.body().getObject("result").getFieldNames()) {
						JsonObject e = res.body().getObject("result").getObject(attr);
						if ("ENSEIGNANT".equals(e.getString("type"))) {
							e.putString("type", "PERSEDUCNAT");
						}
						addPersonClass(e);
						System.out.println(e.encode());
						sendWP(e);
					}
				}
			}
		});
	}

	public static void sendUser(final EventBus eb, final JsonObject user) {
		String query =
				"START n=node:node_auto_index(id={id}) " +
				"MATCH n-[:APPARTIENT]->g-[:DEPENDS]->c " +
				"WHERE has(c.type) AND c.type = 'CLASSE' " +
				"RETURN c.wpId as wpId";
		JsonObject params = new JsonObject()
		.putString("id", user.getString("id"));
		JsonObject jo = new JsonObject();
		jo.putString("action", "execute");
		jo.putString("query", query);
		jo.putObject("params", params);
		eb.send("wse.neo4j.persistor", jo, new Handler<Message<JsonObject>>() {

			@Override
			public void handle(Message<JsonObject> res) {
				if ("ok".equals(res.body().getString("status"))) {
					JsonArray classes = new JsonArray();
					for (String attr : res.body().getObject("result").getFieldNames()) {
						JsonObject j = res.body().getObject("result").getObject(attr);
						classes.add(j.getString("wpId"));
					}
					if ("ENSEIGNANT".equals(user.getString("type"))) {
						user.putString("type", "PERSEDUCNAT");
					}
					JsonObject json = new JsonObject()
					.putString("action", "MANUAL_USER")
					.putArray("classes", classes)
					.putObject("user", user);
					eb.send("wpconnector.address", json);
				}
			}
		});
	}

	private void sendWP(JsonObject jo, Handler<Message<JsonObject>> handler) {
		eb.send("wpconnector.address", jo, handler);
	}

	private void sendWP(JsonObject jo) {
		eb.send("wpconnector.address", jo);
	}
}