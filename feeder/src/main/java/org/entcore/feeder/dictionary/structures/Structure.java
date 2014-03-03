package org.entcore.feeder.dictionary.structures;

import org.entcore.feeder.utils.Neo4j;
import org.entcore.feeder.utils.TransactionHelper;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;

import java.util.*;

public class Structure {

	protected final String id;
	protected final String externalId;
	protected final Importer importer = Importer.getInstance();
	protected JsonObject struct;
	protected final Set<String> classes = Collections.synchronizedSet(new HashSet<String>());
	protected final Set<String> functionalGroups = Collections.synchronizedSet(new HashSet<String>());

	protected Structure(JsonObject struct) {
		this(struct.getString("externalId"), struct);
	}

	protected Structure(JsonObject struct, JsonArray groups, JsonArray classes) {
		this(struct);
		if (groups != null) {
			for (Object o : groups) {
				if (!(o instanceof String)) continue;
				functionalGroups.add((String) o);
			}
		}
		if (classes != null) {
			for (Object o : classes) {
				if (!(o instanceof String)) continue;
				this.classes.add((String) o);
			}
		}
	}

	protected Structure(String externalId, JsonObject struct) {
		if (struct != null && externalId != null && externalId.equals(struct.getString("externalId"))) {
			this.id = struct.getString("id");
		} else {
			throw new IllegalArgumentException("Invalid structure with externalId : " + externalId);
		}
		this.externalId = externalId;
		this.struct = struct;
	}

	private TransactionHelper getTransaction() {
		return importer.getTransaction();
	}

	public void update(JsonObject struct) {
		if (this.struct.equals(struct)) {
			return;
		}
		String query =
				"MATCH (s:Structure { id : {id}}) " +
				"WITH s " +
				"WHERE s.checksum IS NULL OR s.checksum <> {checksum} " +
				"SET " + Neo4j.nodeSetPropertiesFromJson("s", struct, "id", "externalId");
		getTransaction().add(query, struct);
		this.struct = struct;
	}

	public void create() {
		String query =
				"CREATE (s:Structure {props}) " +
				"WITH s " +
				"MATCH (p:Profile) " +
				"CREATE p<-[:HAS_PROFILE]-(g:Group:ProfileGroup {name : s.name+'-'+p.name})-[:DEPENDS]->s " +
				"SET g.id = id(g)+'-'+timestamp() ";
		JsonObject params = new JsonObject()
				.putString("id", id)
				.putString("externalId", externalId)
				.putObject("props", struct);
		getTransaction().add(query, params);
	}

	public void addAttachment() {
		JsonArray functionalAttachment = struct.getArray("functionalAttachment");
		if (functionalAttachment != null && functionalAttachment.size() > 0 &&
				!externalId.equals(functionalAttachment.get(0))) {
			JsonObject params = new JsonObject().putString("externalId", externalId);
			String query;
			if (functionalAttachment.size() == 1) {
				query =
						"MATCH (s:Structure { externalId : {externalId}}), " +
						"(ps:Structure { externalId : {functionalAttachment}}) " +
						"CREATE UNIQUE s-[:HAS_ATTACHMENT]->ps";
				params.putString("functionalAttachment", (String) functionalAttachment.get(0));
			} else {
				query =
						"MATCH (s:Structure { externalId : {externalId}}), (ps:Structure) " +
						"WHERE ps.externalId IN {functionalAttachment} " +
						"CREATE UNIQUE s-[:HAS_ATTACHMENT]->ps";
				params.putArray("functionalAttachment", functionalAttachment);
			}
			getTransaction().add(query, params);
		}
	}

	public void createClassIfAbsent(String classExternalId, String name) {
		if (classes.add(classExternalId)) {
			String query =
					"MATCH (s:Structure { externalId : {structureExternalId}}) " +
					"CREATE s<-[:BELONGS]-(c:Class {props})" +
					"WITH s, c " +
					"MATCH s<-[:DEPENDS]-(g:ProfileGroup)-[:HAS_PROFILE]->(p:Profile) " +
					"CREATE c<-[:DEPENDS]-(pg:Group:ProfileGroup {name : c.name+'-'+p.name})-[:DEPENDS]->g " +
					"SET pg.id = id(pg)+'-'+timestamp() ";
			JsonObject params = new JsonObject()
					.putString("structureExternalId", externalId)
					.putObject("props", new JsonObject()
							.putString("externalId", classExternalId)
							.putString("id", UUID.randomUUID().toString())
							.putString("name", name)
					);
			getTransaction().add(query, params);
		}
	}

	public void createFunctionalGroupIfAbsent(String groupExternalId, String name) {
		if (functionalGroups.add(groupExternalId)) {
			String query =
					"MATCH (s:Structure { externalId : {structureExternalId}}) " +
					"CREATE s<-[:DEPENDS]-(c:Group:FunctionalGroup {props}) ";
			JsonObject params = new JsonObject()
					.putString("structureExternalId", externalId)
					.putObject("props", new JsonObject()
							.putString("externalId", groupExternalId)
							.putString("id", UUID.randomUUID().toString())
							.putString("name", name)
					);
			getTransaction().add(query, params);
		}
	}

	public void linkModules(String moduleExternalId) {
		String query =
				"MATCH (s:Structure { externalId : {externalId}}), " +
				"(m:Module { externalId : {moduleExternalId}}) " +
				"CREATE UNIQUE s-[:OFFERS]->m";
		JsonObject params = new JsonObject()
				.putString("externalId", externalId)
				.putString("moduleExternalId", moduleExternalId);
		getTransaction().add(query, params);
	}

	public void linkClassFieldOfStudy(String classExternalId, String fieldOfStudyExternalId) {
		String query =
				"MATCH (s:Structure { externalId : {externalId}})" +
				"<-[:BELONGS]-(c:Class { externalId : {classExternalId}}), " +
				"(f:FieldOfStudy { externalId : {fieldOfStudyExternalId}}) " +
				"CREATE UNIQUE c-[:TEACHES]->f";
		JsonObject params = new JsonObject()
				.putString("externalId", externalId)
				.putString("classExternalId", classExternalId)
				.putString("fieldOfStudyExternalId", fieldOfStudyExternalId);
		getTransaction().add(query, params);
	}

	public void linkGroupFieldOfStudy(String groupExternalId, String fieldOfStudyExternalId) {
		String query =
				"MATCH (s:Structure { externalId : {externalId}})" +
				"<-[:DEPENDS]-(c:FunctionalGroup { externalId : {groupExternalId}}), " +
				"(f:FieldOfStudy { externalId : {fieldOfStudyExternalId}}) " +
				"CREATE UNIQUE c-[:TEACHES]->f";
		JsonObject params = new JsonObject()
				.putString("externalId", externalId)
				.putString("groupExternalId", groupExternalId)
				.putString("fieldOfStudyExternalId", fieldOfStudyExternalId);
		getTransaction().add(query, params);
	}

}
