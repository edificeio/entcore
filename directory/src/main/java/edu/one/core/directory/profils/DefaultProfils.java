package edu.one.core.directory.profils;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.vertx.java.core.Handler;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.JsonObject;

import edu.one.core.infra.Neo;

public class DefaultProfils implements Profils {

	private final Neo neo;

	public DefaultProfils(Neo neo) {
		this.neo = neo;
	}

	@Override
	public void createGroupProfil(String profil, final Handler<JsonObject> handler) {
		final Map<String, Object> params = new HashMap<String, Object>();
		params.put("profil", profil);
		params.put("type", "GROUPE");
		params.put("id", UUID.randomUUID().toString());
		params.put("name", profil + "S");
		neo.send(
				"START n=node(*) WHERE has(n.ENTGroupeNom) AND n.ENTGroupeNom={name} "+
				"WITH count(*) AS exists " +
				"WHERE exists=0 " +
				"CREATE (m {id:{id}, " +
					"type:{type}, " +
					"ENTGroupeNom:{name}, " +
					"ENTPeople:'[]'}) " +
				"RETURN m",
				params,
				new Handler<Message<JsonObject>>() {

					@Override
					public void handle(Message<JsonObject> r) {
						if ("ok".equals(r.body().getString("status"))) {
						neo.send(
								"START n=node:node_auto_index(type={profil}), " +
								"m=node:node_auto_index(id={id}) " +
								"CREATE UNIQUE n-[r:APPARTIENT]->m " +
								"RETURN r",
								params,
								new Handler<Message<JsonObject>>() {

									@Override
									public void handle(Message<JsonObject> res) {
										handler.handle(res.body());
									}
								}
							);
						} else {
							handler.handle(r.body());
						}
					}
				});
	}

	@Override
	public void listGroupsProfils(final Handler<JsonObject> handler) {
		Map<String, Object> params = new HashMap<String, Object>();
		params.put("type","GROUPE");
		neo.send(
			"START n=node:node_auto_index(type={type}) " +
			"RETURN distinct n.ENTGroupeNom as name, n.id as id",
			params,
			new Handler<Message<JsonObject>>() {

				@Override
				public void handle(Message<JsonObject> res) {
					handler.handle(res.body());
				}
			}
		);
	}

}
