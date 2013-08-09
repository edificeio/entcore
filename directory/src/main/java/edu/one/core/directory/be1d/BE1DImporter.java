package edu.one.core.directory.be1d;

import static edu.one.core.directory.be1d.BE1DConstants.*;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.vertx.java.core.Handler;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.platform.Container;

import au.com.bytecode.opencsv.CSV;
import au.com.bytecode.opencsv.CSVReadProc;

import com.google.common.base.Joiner;

import edu.one.core.datadictionary.generation.IdGenerator;
import edu.one.core.datadictionary.generation.LoginGenerator;
import edu.one.core.datadictionary.generation.PasswordGenerator;
import edu.one.core.infra.Neo;

public class BE1DImporter {

	private final String schoolFolder;
	private final CSV csv;
	private int count = 0;
	private final JsonArray queries;
	private final Map<String, Tuple<ArrayList<String>>> classesEleves;
	private final Map<String, String> mappingEleveId;
	private final List<JsonObject> parentsEnfantsMapping;
	private final List<JsonObject> classesGroupProfilsMapping;
	private final Neo neo;
	private final PasswordGenerator pwGenerator;
	private final IdGenerator idGenerator;
	private final LoginGenerator loginGenerator;

	class Tuple<T> {
		private final String s1;
		private final T s2;
		public Tuple(String s1, T s2) {
			this.s1 = s1;
			this.s2 = s2;
		}
		public String getS1() {
			return s1;
		}
		public T getS2() {
			return s2;
		}
	}

	private static final String createClass =
			"CREATE (c { id: {id}, type: 'CLASSE', ENTGroupeNom: {name} })";
	private static final String createGroupProfil =
			"CREATE (c { id: {id}, type: {type}, name: {name} })";
	private static final String createRelEN_RELATION_AVEC =
			"START n=node:node_auto_index(id={childId}), " +
			"m=node:node_auto_index(id={parentId}) " +
			"CREATE UNIQUE n-[:EN_RELATION_AVEC]->m ";
	private static final String createRelsAppartientGroupEleve =
			"START n=node:node_auto_index(id={classId}), " +
			"m=node:node_auto_index(id={groupId}) " +
			"MATCH n<-[:APPARTIENT]-e " +
			"CREATE UNIQUE e-[:APPARTIENT]->m";
	private static final String createRelsAppartientGroupParent =
			"START n=node:node_auto_index(id={classId}), " +
			"m=node:node_auto_index(id={parentGroupId}) " +
			"MATCH n<-[:APPARTIENT]-e-[:EN_RELATION_AVEC]->p " +
			"CREATE UNIQUE p-[:APPARTIENT]->m";
	private static final String createRelsDepends =
			"START n=node:node_auto_index({groupsIds}), " +
			"m=node:node_auto_index(id={groupId}) " +
			"CREATE UNIQUE n-[:DEPENDS]->m";
	private static String createRelsAppartient(List<String> userIds) {
		return  "START n=node:node_auto_index('id:"+ Joiner.on(" OR id:").join(userIds) + "'), " +
				"m=node:node_auto_index(id={groupId}) " +
				"CREATE UNIQUE n-[:APPARTIENT]->m ";
	}
	private static String createEntity(JsonObject json) {
		StringBuilder sb = new StringBuilder();
		sb.append("CREATE (c { ");
		for (String attr : json.getFieldNames()) {
			sb.append(attr + ":{" + attr + "}, ");
		}
		sb.delete(sb.lastIndexOf(","), sb.length());
		return sb.toString() + "})";
	}

	public BE1DImporter(Vertx vertx, Container container, String schoolFolder) {
		neo = new Neo(vertx.eventBus(), container.logger());
		this.schoolFolder = schoolFolder;
		classesEleves = new HashMap<>();
		mappingEleveId = new HashMap<>();
		parentsEnfantsMapping = new ArrayList<>();
		classesGroupProfilsMapping = new ArrayList<>();
		queries = new JsonArray();
		loginGenerator = new LoginGenerator();
		pwGenerator = new PasswordGenerator();
		idGenerator = new IdGenerator();
		csv = CSV.separator(';').quote('\"').skipLines(1).charset("ISO-8859-1").create();
		//charset("UTF-8").create();
	}


	public void importSchool(final String schoolName, final Handler<JsonObject> handler) {
		String fileEleves = schoolFolder + File.separator + "CSVExtraction-eleves.csv";
		String fileParents = schoolFolder + File.separator + "CSVExtraction-responsables.csv";

		createSchool(schoolName);

		extractEleves(fileEleves);

		if (mappingEleveId.size() != count) {
			int nb = count - mappingEleveId.size();
			throw new IllegalArgumentException(
					"tuple nom+prenom+classe is not unique for " + nb + " users.");
		}

		extractParents(fileParents);

		for (JsonObject mapping: parentsEnfantsMapping) {
			queries.add(new JsonObject()
				.putString("query", createRelEN_RELATION_AVEC)
				.putObject("params", mapping));
		}

		List<String> childsIds = new ArrayList<>();
		List<String> classIds = new ArrayList<>();
		for (Tuple<ArrayList<String>> t: classesEleves.values()) {
			queries.add(new JsonObject()
			.putString("query", createRelsAppartient(t.getS2()))
			.putObject("params", new JsonObject()
			.putString("groupId", t.getS1())));
			classIds.add(t.getS1());
			childsIds.addAll(t.getS2());
		}
		String schoolId = ((JsonObject) queries.get(0)).getObject("params").getString("id");

		// relationship between school and childs
		queries.add(new JsonObject()
		.putString("query", createRelsAppartient(childsIds))
		.putObject("params", new JsonObject()
		.putString("groupId", schoolId)));

		// relationship between school group profil eleve and childs
		String gpcId = ((JsonObject) queries.get(1)).getObject("params").getString("id");
		queries.add(new JsonObject()
		.putString("query", createRelsAppartientGroupEleve)
		.putObject("params", new JsonObject()
		.putString("classId", schoolId)
		.putString("groupId", gpcId)));

		// relationship between school group profil PERSRELELEVE and parents
		String gppId = ((JsonObject) queries.get(2)).getObject("params").getString("id");
		queries.add(new JsonObject()
		.putString("query", createRelsAppartientGroupParent)
		.putObject("params", new JsonObject()
		.putString("classId", schoolId)
		.putString("parentGroupId", gppId)));

		// relationship between school and class
		queries.add(new JsonObject()
		.putString("query", createRelsAppartient(classIds))
		.putObject("params", new JsonObject()
		.putString("groupId", schoolId)));

		// relationship between school and group profil school
		queries.add(new JsonObject()
		.putString("query", createRelsDepends)
		.putObject("params", new JsonObject()
		.putString("groupId", schoolId)
		.putString("groupsIds", "id:" + gppId + " OR id:" + gpcId)));

		List<String> groupsPP = new ArrayList<>();
		List<String> groupsPC = new ArrayList<>();
		for (JsonObject mapping: classesGroupProfilsMapping) {
			queries.add(new JsonObject()
				.putString("query", createRelsAppartientGroupEleve)
				.putObject("params", mapping));
			queries.add(new JsonObject()
			.putString("query", createRelsAppartientGroupParent)
			.putObject("params", mapping));
			queries.add(new JsonObject()
			.putString("query", createRelsDepends)
			.putObject("params", new JsonObject()
			.putString("groupId", mapping.getString("classId"))
			.putString("groupsIds", "id:" + mapping.getString("groupId") +
					" OR id:" + mapping.getString("parentGroupId"))));
			groupsPC.add(mapping.getString("groupId"));
			groupsPP.add(mapping.getString("parentGroupId"));
		}

		queries.add(new JsonObject()
		.putString("query", createRelsDepends)
		.putObject("params", new JsonObject()
		.putString("groupId", gppId)
		.putString("groupsIds", "id:" + Joiner.on(" OR id:").join(groupsPP))));

		queries.add(new JsonObject()
		.putString("query", createRelsDepends)
		.putObject("params", new JsonObject()
		.putString("groupId", gpcId)
		.putString("groupsIds", "id:" + Joiner.on(" OR id:").join(groupsPC))));

		neo.sendBatch(queries, new Handler<Message<JsonObject>>() {

			@Override
			public void handle(Message<JsonObject> m) {
				handler.handle(new JsonObject().putObject(schoolName, m.body()));
			}
		});
	}

	private void createSchool(final String schoolName) {
		JsonObject school = new JsonObject()
		.putString("id", UUID.randomUUID().toString())
		.putString("ENTStructureNomCourant", schoolName)
		.putString("type", "ETABEDUCNAT");
		queries.add(new JsonObject()
		.putString("query", createEntity(school))
		.putObject("params", school));
		queries.add(new JsonObject()
		.putString("query", createGroupProfil)
		.putObject("params", new JsonObject()
		.putString("id", UUID.randomUUID().toString())
		.putString("name", schoolName +"_ELEVE")
		.putString("type", "GROUP_ETABEDUCNAT_ELEVE")));
		queries.add(new JsonObject()
		.putString("query", createGroupProfil)
		.putObject("params", new JsonObject()
		.putString("id", UUID.randomUUID().toString())
		.putString("name", schoolName + "_PERSRELELEVE")
		.putString("type", "GROUP_ETABEDUCNAT_PERSRELELEVE")));
	}

	private void extractParents(String fileParents) {
		csv.read(fileParents, new CSVReadProc() {

			@Override
			public void procRow(int rowIdx, String... values) {
				String id = UUID.randomUUID().toString();
				JsonObject row = new JsonObject()
				.putString("id", id)
				.putString("type", "PERSRELELEVE")
				.putString(ENTPersonAdresse, values[RespENTPersonAdresseIdx])
				.putString(ENTPersonCivilite, values[RespENTPersonCiviliteIdx])
				.putString(ENTPersonCodePostal, values[RespENTPersonCodePostalIdx])
				.putString(ENTPersonMail, values[RespENTPersonMailIdx])
				.putString(ENTPersonNom, values[RespENTPersonNomIdx])
				.putString(ENTPersonNomPatro, values[RespENTPersonNomPatroIdx])
				.putString(ENTPersonPays, values[RespENTPersonPaysIdx])
				.putString(ENTPersonPrenom, values[RespENTPersonPrenomIdx])
				.putString(ENTPersonTelPerso, values[RespENTPersonTelPerso])
				.putString(ENTPersonVille, values[RespENTPersonVilleIdx])
				.putString(ENTPersRelEleveTelMobile, values[RespENTPersRelEleveTelMobile])
				.putString(ENTPersRelElevTelPro, values[RespENTPersRelElevTelPro])
				.putString(ENTPersonIdentifiant, idGenerator.generate())
				.putString(ENTPersonLogin, loginGenerator
						.generate(values[ENTPersonPrenomIdx], values[ENTPersonNomIdx]))
				.putString(ENTPersonMotDePasse, pwGenerator.generate());

				for (int i = 13; i < values.length; i += 4) {
					String mapping = values[i]+values[i+1]+values[i+2]+values[i+3];
					String t = mappingEleveId.get(mapping);
					if (t == null) {
						throw new IllegalArgumentException(
								"tuple nom+prenom+classe is empty for PERSRELELEVE at line " + rowIdx + ".");
					}
					parentsEnfantsMapping.add(new JsonObject()
					.putString("childId", t)
					.putString("parentId", id));
				}
				queries.add(new JsonObject()
				.putString("query", createEntity(row))
				.putObject("params", row));
			}
		});
	}

	private void extractEleves(String fileEleves) {
		csv.read(fileEleves, new CSVReadProc() {

			@Override
			public void procRow(int rowIdx, String... values) {
				String id = UUID.randomUUID().toString();
				JsonObject row = new JsonObject()
				.putString("id", id)
				.putString("type", "ELEVE")
				.putString(ENTEleveCycle, values[ENTEleveCycleIdx])
				.putString(ENTEleveNiveau, values[ENTEleveNiveauIdx])
				.putString(ENTPersonAdresse, values[ENTPersonAdresseIdx])
				.putString(ENTPersonClasses, values[ENTPersonClassesIdx])
				.putString(ENTPersonCodePostal, values[ENTPersonCodePostalIdx])
				.putString(ENTPersonDateNaissance, values[ENTPersonDateNaissanceIdx])
				.putString(ENTPersonNom, values[ENTPersonNomIdx])
				.putString(ENTPersonNomPatro, values[ENTPersonNomPatroIdx])
				.putString(ENTPersonPays, values[ENTPersonPaysIdx])
				.putString(ENTPersonPrenom, values[ENTPersonPrenomIdx])
				.putString(ENTPersonSexe, values[ENTPersonSexeIdx])
				.putString(ENTPersonVille, values[ENTPersonVilleIdx])
				.putString(ENTPersonIdentifiant, idGenerator.generate())
				.putString(ENTPersonLogin, loginGenerator
						.generate(values[ENTPersonPrenomIdx], values[ENTPersonNomIdx]))
				.putString(ENTPersonMotDePasse, pwGenerator.generate());

				Tuple<ArrayList<String>> eleves = classesEleves.get(values[ENTPersonClassesIdx]);
				if (eleves == null) {
					eleves = new Tuple<>(UUID.randomUUID().toString(), new ArrayList<String>());
					classesEleves.put(values[ENTPersonClassesIdx], eleves);
					queries.add(new JsonObject()
					.putString("query", createClass)
					.putObject("params", new JsonObject()
					.putString("id", eleves.getS1())
					.putString("name", values[ENTPersonClassesIdx])));
					String gId = UUID.randomUUID().toString();
					queries.add(new JsonObject()
					.putString("query", createGroupProfil)
					.putObject("params", new JsonObject()
					.putString("id", gId)
					.putString("name", values[ENTPersonClassesIdx]+"_ELEVE")
					.putString("type", "GROUP_CLASSE_ELEVE")));
					String gpId = UUID.randomUUID().toString();
					queries.add(new JsonObject()
					.putString("query", createGroupProfil)
					.putObject("params", new JsonObject()
					.putString("id", gpId)
					.putString("name", values[ENTPersonClassesIdx]+"_PERSRELELEVE")
					.putString("type", "GROUP_CLASSE_PERSRELELEVE")));
					classesGroupProfilsMapping.add(new JsonObject()
							.putString("classId", eleves.getS1())
							.putString("groupId", gId)
							.putString("parentGroupId", gpId));
				}
				eleves.getS2().add(id);
				String mapping = values[ENTPersonNomPatroIdx]+values[ENTPersonNomIdx]
						+values[ENTPersonPrenomIdx]+values[ENTPersonClassesIdx];
				mappingEleveId.put(mapping, id);
				queries.add(new JsonObject()
				.putString("query", createEntity(row))
				.putObject("params", row));
				count++;
			}
		});
	}

}
