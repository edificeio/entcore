package edu.one.core.directory.be1d;

import static edu.one.core.directory.be1d.BE1DConstants.*;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
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

import edu.one.core.datadictionary.generation.ActivationCodeGenerator;
import edu.one.core.datadictionary.generation.DisplayNameGenerator;
import edu.one.core.datadictionary.generation.IdGenerator;
import edu.one.core.datadictionary.generation.LoginGenerator;
import edu.one.core.common.neo4j.Neo;
import edu.one.core.infra.Server;

public class BE1DImporter {

	private final String schoolFolder;
	private final CSV csv;
	private int count = 0;
	private final JsonArray queries;
	private final JsonArray queriesCom;
	private final Map<String, Tuple<ArrayList<String>>> classesEleves;
	private final Map<String, String> mappingEleveId;
	private final List<JsonObject> parentsEnfantsMapping;
	private final List<JsonObject> classesGroupProfilsMapping;
	private final Neo neo;
	private final ActivationCodeGenerator activationGenerator;
	private final IdGenerator idGenerator;
	private final LoginGenerator loginGenerator;
	private final DisplayNameGenerator displayNameGenerator;

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
			"CREATE (c:Class { id: {id}, name: {name} })";
	private static String createGroupProfil(String type) {
		if (type != null && type.startsWith("Class")) {
			return "CREATE (c:ProfileGroup:ClassProfileGroup:"+type+" { id: {id}, name: {name} })";
		} else {
			return "CREATE (c:ProfileGroup:SchoolProfileGroup:"+type+" { id: {id}, name: {name} })";
		}
	}
	private static final String createRelEN_RELATION_AVEC =
			"MATCH (n:Student), (m:Relative) " +
			"WHERE n.id = {childId} AND m.id = {parentId} " +
			"CREATE UNIQUE n-[:EN_RELATION_AVEC]->m ";
	private static final String createRelsAppartientGroupEleve =
			"MATCH (n)<-[:APPARTIENT]-e, (m:ProfileGroup) " +
			"WHERE (n:Class OR n:School) AND n.id = {classId} AND m.id = {groupId} " +
			"CREATE UNIQUE e-[:APPARTIENT]->m";
	private static final String createRelsAppartientGroupParent =
			"MATCH (n)<-[:APPARTIENT]-(e:Student)-[:EN_RELATION_AVEC]->(p:Relative), (m:ProfileGroup) " +
			"WHERE (n:Class OR n:School) AND n.id = {classId} AND m.id = {parentGroupId} " +
			"CREATE UNIQUE p-[:APPARTIENT]->m";
	private static String createRelsDepends(String groupsIds) {
//			"START n=node:node_auto_index({groupsIds}), " +
//			"m=node:node_auto_index(id={groupId}) " +
		return "MATCH (n:ProfileGroup), (m) "+
			"WHERE n.id IN ['" + groupsIds + "'] AND (m:Class OR m:School OR m:ProfileGroup) AND m.id = {groupId} " +
			"CREATE UNIQUE n-[:DEPENDS]->m";
	}
	private static String createRelsAppartientGroup(String nodeIds) {
		return "MATCH (n), (m) " +
			"WHERE (m:ProfileGroup OR m:Class OR m:User) AND m.id IN ['"+nodeIds+"'] " +
			"AND (n:ProfileGroup OR n:Class OR n:School) AND n.id = {classId} " +
			"CREATE UNIQUE m-[:APPARTIENT]->n";
	}
	private static String createRelsAppartient(List<String> userIds) {
		return  "MATCH (n), (m) " +
				"WHERE (n:User OR n:Class) AND n.id IN ['"+ Joiner.on("','").join(userIds) + "'] " +
				"AND (m:ProfileGroup OR m:Class OR m:School) AND m.id = {groupId} " +
				"CREATE UNIQUE n-[:APPARTIENT]->m ";
	}
	private static String createEntity(JsonObject json, String type) {
		StringBuilder sb = new StringBuilder();
		sb.append("CREATE (c:").append(type).append(" { ");
		for (String attr : json.getFieldNames()) {
			sb.append(attr).append(":{").append(attr).append("}, ");
		}
		sb.delete(sb.lastIndexOf(","), sb.length());
		return sb.toString() + "})";
	}

	public BE1DImporter(Vertx vertx, Container container, String schoolFolder) {
		neo = new Neo(Server.getEventBus(vertx), container.logger());
		this.schoolFolder = schoolFolder;
		classesEleves = new HashMap<>();
		mappingEleveId = new HashMap<>();
		parentsEnfantsMapping = new ArrayList<>();
		classesGroupProfilsMapping = new ArrayList<>();
		queries = new JsonArray();
		queriesCom = new JsonArray();
		loginGenerator = new LoginGenerator();
		activationGenerator = new ActivationCodeGenerator();
		idGenerator = new IdGenerator();
		displayNameGenerator = new DisplayNameGenerator();
		csv = CSV.separator(';').quote('\"').skipLines(1).charset("ISO-8859-1").create();
		//charset("UTF-8").create();
	}


	public void importSchool(final String schoolName, String UAI, final Handler<JsonObject> handler) {
		String fileEleves = schoolFolder + File.separator + "CSVExtraction-eleves.csv";
		String fileParents = schoolFolder + File.separator + "CSVExtraction-responsables.csv";
		String fileTeachers = schoolFolder + File.separator + "CSVExtraction-enseignants.csv";

		createSchool(schoolName, UAI);

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
			defaultParentsChildsCom(mapping);
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
		final String schoolId = ((JsonObject) queries.get(0)).getObject("params").getString("id");

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
		.putString("query", createRelsDepends(gppId + "','" + gpcId))
		.putObject("params", new JsonObject()
		.putString("groupId", schoolId)));

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
			.putString("query", createRelsDepends(mapping.getString("groupId") +
					"','" + mapping.getString("parentGroupId")))
			.putObject("params", new JsonObject()
			.putString("groupId", mapping.getString("classId"))));
			groupsPC.add(mapping.getString("groupId"));
			groupsPP.add(mapping.getString("parentGroupId"));
			defaultInsideGroupCom(mapping.getString("groupId"));
			defaultInsideGroupCom(mapping.getString("parentGroupId"));
		}

		queries.add(new JsonObject()
		.putString("query", createRelsDepends(Joiner.on("','").join(groupsPP)))
		.putObject("params", new JsonObject()
		.putString("groupId", gppId)));

		queries.add(new JsonObject()
		.putString("query", createRelsDepends(Joiner.on("','").join(groupsPC)))
		.putObject("params", new JsonObject()
		.putString("groupId", gpcId)));

		extractTeachers(fileTeachers);

		neo.sendBatch(queries, new Handler<Message<JsonObject>>() {

			@Override
			public void handle(final Message<JsonObject> m) {
				if ("ok".equals(m.body().getString("status"))) {
					neo.sendBatch(queriesCom, new Handler<Message<JsonObject>>() {

						@Override
						public void handle(Message<JsonObject> message) {
							handler.handle(new JsonObject().putObject(schoolName, m.body()));
						}
					});
				} else {
					handler.handle(new JsonObject().putObject(schoolName, m.body()));
				}
			}
		});
	}

	private void createSchool(final String schoolName, String UAI) {
		JsonObject school = new JsonObject()
		.putString("id", UUID.randomUUID().toString())
		.putString("name", schoolName)
		.putString("UAI", UAI);
		queries.add(new JsonObject()
		.putString("query", createEntity(school, "School"))
		.putObject("params", school));
		queries.add(new JsonObject()
		.putString("query", createGroupProfil("SchoolStudentGroup"))
		.putObject("params", new JsonObject()
		.putString("id", UUID.randomUUID().toString())
		.putString("name", schoolName +"_ELEVE")));
		queries.add(new JsonObject()
		.putString("query", createGroupProfil("SchoolRelativeGroup"))
		.putObject("params", new JsonObject()
		.putString("id", UUID.randomUUID().toString())
		.putString("name", schoolName + "_PERSRELELEVE")));
		queries.add(new JsonObject()
		.putString("query", createGroupProfil("SchoolTeacherGroup"))
		.putObject("params", new JsonObject()
		.putString("id", UUID.randomUUID().toString())
		.putString("name", schoolName + "_ENSEIGNANT")));
		queries.add(new JsonObject()
		.putString("query", createGroupProfil("SchoolPrincipalGroup"))
		.putObject("params", new JsonObject()
		.putString("id", UUID.randomUUID().toString())
		.putString("name", schoolName + "_DIRECTEUR")));
	}


	private void extractTeachers(String fileTeachers) {
		final Map<String, List<String>> classesTeachers = new HashMap<>();
		final List<String> directors = new ArrayList<>();
		csv.read(fileTeachers, new CSVReadProc() {

			@Override
			public void procRow(int rowIdx, String... values) {
				String id = UUID.randomUUID().toString();
				JsonObject row = new JsonObject()
				.putString("id", id)
				.putString(ENTPersonAdresse, values[TeacherENTPersonAdresseIdx])
				.putString(ENTPersonCivilite, values[TeacherENTPersonCiviliteIdx])
				.putString(ENTPersonCodePostal, values[TeacherENTPersonCodePostalIdx])
				.putString(ENTPersonMail, values[TeacherENTPersonMailIdx])
				.putString(ENTPersonNom, values[TeacherENTPersonNomIdx])
				.putString(ENTPersonNomPatro, values[TeacherENTPersonNomPatroIdx])
				.putString(ENTPersonPays, values[TeacherENTPersonPaysIdx])
				.putString(ENTPersonPrenom, values[TeacherENTPersonPrenomIdx])
				.putString(ENTPersonTelPerso, values[TeacherENTPersonTelPerso])
				.putString(ENTPersonVille, values[TeacherENTPersonVilleIdx])
				.putString(ENTPersRelEleveTelMobile, values[TeacherENTPersRelEleveTelMobile])
				.putString(ENTPersonIdentifiant, idGenerator.generate())
				.putString(ENTPersonLogin, loginGenerator
						.generate(values[TeacherENTPersonPrenomIdx], values[TeacherENTPersonNomIdx]))
				.putString(ENTPersonNomAffichage, displayNameGenerator
						.generate(values[TeacherENTPersonPrenomIdx], values[TeacherENTPersonNomIdx]))
				.putString("activationCode", activationGenerator.generate());

				if (values[TeacherENTEnsFonctionDir] != null &&
						"OUI".equals(values[TeacherENTEnsFonctionDir].trim().toUpperCase())) {
					row.putBoolean(ENTEnsFonctionDir, true);
					directors.add(id);
				}

				final StringBuilder sb = new StringBuilder();
				for (int i = 12; i < values.length; i++) {
					String mapping = values[i];
					if (mapping == null || mapping.trim().isEmpty()) continue;
					if (!classesEleves.containsKey(mapping)) {
						throw new IllegalArgumentException(
								"Invalid classe for teacher at line " + rowIdx);
					}
					sb.append("|").append(mapping);
					List<String> l = classesTeachers.get(mapping);
					if (l == null) {
						l = new ArrayList<>();
						classesTeachers.put(mapping, l);
					}
					l.add(id);
				}
				if (sb.length() > 0) {
					row.putArray(ENTPersonClasses, new JsonArray(sb.substring(1).split("\\|")));
					createUser(row, "User:Teacher");
				} else {
					row.putArray(ENTPersonClasses, new JsonArray());
					createUser(row, "User:Teacher");
					relationshipTeachersWithoutClass(id);
				}
			}
		});
		relationshipTeachers(classesTeachers, directors);
	}

	private void relationshipTeachersWithoutClass(String teacherId) {
		String schoolId = ((JsonObject) queries.get(0)).getObject("params").getString("id");
		String gId = ((JsonObject) queries.get(3)).getObject("params").getString("id");
		queries.add(toJsonObject(createRelsAppartientGroup(teacherId), new JsonObject()
		.putString("classId", schoolId)));
		queries.add(toJsonObject(createRelsAppartientGroup(teacherId), new JsonObject()
		.putString("classId", gId)));
	}

	private void relationshipTeachers(Map<String, List<String>> classesTeachers, List<String> directors) {
		StringBuilder sb = new StringBuilder();
		List<String> gcId = new ArrayList<>();
		for (Entry<String, List<String>> e: classesTeachers.entrySet()) {
			String groupId = UUID.randomUUID().toString();
			queries.add(toJsonObject(createGroupProfil("ClassTeacherGroup"), new JsonObject()
			.putString("id", groupId)
			.putString("name", e.getKey() + "_ENSEIGNANT")));
			String ids = Joiner.on("','").join(e.getValue());
			queries.add(toJsonObject(createRelsAppartientGroup(ids), new JsonObject()
			.putString("classId", groupId)));
			queries.add(toJsonObject(createRelsAppartientGroup(ids), new JsonObject()
			.putString("classId", classesEleves.get(e.getKey()).getS1())));
			sb.append("','").append(ids);
			queries.add(toJsonObject(createRelsDepends(groupId), new JsonObject()
					.putString("groupId", classesEleves.get(e.getKey()).getS1())));
			defaultInsideGroupCom(groupId);
			defaultTeacherClassCom(groupId);
			gcId.add(groupId);
		}
		String schoolId = ((JsonObject) queries.get(0)).getObject("params").getString("id");
		String gId = ((JsonObject) queries.get(3)).getObject("params").getString("id");

		queries.add(toJsonObject(createRelsAppartientGroup(sb.substring(3)), new JsonObject()
		.putString("classId", schoolId)));

		queries.add(toJsonObject(createRelsAppartientGroup(sb.substring(3)), new JsonObject()
		.putString("classId", gId)));

		queries.add(toJsonObject(createRelsDepends(gId), new JsonObject()
		.putString("groupId", schoolId)));

		queries.add(toJsonObject(createRelsDepends(Joiner.on("','").join(gcId)), new JsonObject()
		.putString("groupId", gId)));

		String allStudentGroup = ((JsonObject) queries.get(1)).getObject("params").getString("id");
		String allParentsGroup = ((JsonObject) queries.get(2)).getObject("params").getString("id");
		defaultInsideGroupCom(gId);
		comBetweenGroup(gId, allStudentGroup);
		comBetweenGroup(gId, allParentsGroup);
		defaultOutGroupCom(allStudentGroup);
		defaultOutGroupCom(allParentsGroup);

		// directors
		String directorsId = ((JsonObject) queries.get(4)).getObject("params").getString("id");
		queries.add(toJsonObject(createRelsAppartientGroup(Joiner.on("','").join(directors)), new JsonObject()
		.putString("classId", directorsId)));
		queries.add(toJsonObject(createRelsDepends(directorsId), new JsonObject()
		.putString("groupId", schoolId)));
		defaultInsideGroupCom(directorsId);
	}

	private void extractParents(String fileParents) {
		csv.read(fileParents, new CSVReadProc() {

			@Override
			public void procRow(int rowIdx, String... values) {
				String id = UUID.randomUUID().toString();
				JsonObject row = new JsonObject()
				.putString("id", id)
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
						.generate(values[RespENTPersonPrenomIdx], values[RespENTPersonNomIdx]))
				.putString(ENTPersonNomAffichage, displayNameGenerator
						.generate(values[RespENTPersonPrenomIdx], values[RespENTPersonNomIdx]))
				.putString("activationCode", activationGenerator.generate());

				final StringBuilder sb = new StringBuilder();
				for (int i = 13; i < values.length; i += 4) {
					String mapping = values[i]+values[i+1]+values[i+2]+values[i+3];
					if (mapping.trim().isEmpty()) continue;
					String t = mappingEleveId.get(mapping);
					if (t == null) {
						throw new IllegalArgumentException(
								"tuple nom+prenom+classe is empty for PERSRELELEVE at line " + rowIdx + ".");
					}
					sb.append("|").append(values[i+3]);
					parentsEnfantsMapping.add(new JsonObject()
					.putString("childId", t)
					.putString("parentId", id));
				}
				row.putArray(ENTPersonClasses, new JsonArray(sb.substring(1).split("\\|")));
				createUser(row, "User:Relative");
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
				.putString(ENTEleveCycle, values[ENTEleveCycleIdx])
				.putString(ENTEleveNiveau, values[ENTEleveNiveauIdx])
				.putString(ENTPersonAdresse, values[ENTPersonAdresseIdx])
				.putArray(ENTPersonClasses, new JsonArray().addString(values[ENTPersonClassesIdx]))
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
				.putString(ENTPersonNomAffichage, displayNameGenerator
						.generate(values[ENTPersonPrenomIdx], values[ENTPersonNomIdx]))
				.putString("activationCode", activationGenerator.generate());

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
					.putString("query", createGroupProfil("ClassStudentGroup"))
					.putObject("params", new JsonObject()
					.putString("id", gId)
					.putString("name", values[ENTPersonClassesIdx]+"_ELEVE")));
					String gpId = UUID.randomUUID().toString();
					queries.add(new JsonObject()
					.putString("query", createGroupProfil("ClassRelativeGroup"))
					.putObject("params", new JsonObject()
					.putString("id", gpId)
					.putString("name", values[ENTPersonClassesIdx]+"_PERSRELELEVE")));
					classesGroupProfilsMapping.add(new JsonObject()
							.putString("classId", eleves.getS1())
							.putString("groupId", gId)
							.putString("parentGroupId", gpId));
				}
				eleves.getS2().add(id);
				String mapping = values[ENTPersonNomPatroIdx]+values[ENTPersonNomIdx]
						+values[ENTPersonPrenomIdx]+values[ENTPersonClassesIdx];
				mappingEleveId.put(mapping, id);
				createUser(row, "User:Student");
				count++;
			}
		});
	}

	private void createUser(JsonObject row, String type) {
		String id = row.getString("id");
		String login = row.getString(ENTPersonLogin);
		if (id == null || login == null) {
			throw new IllegalArgumentException("Invalid user : " + row.encode());
		}
		queries.add(new JsonObject()
		.putString("query", createEntity(row, type))
		.putObject("params", row));
		//userLoginUnicity(id, login);
	}

	private void userLoginUnicity(String id, String login) {
		// e.g. login -> 'ENTPersonLogin:tom.mate*'
		String loginUnicity =
			"START m=node:node_auto_index({login}), " +
			"n=node:node_auto_index(id={nodeId}) " +
			"WITH count(m) as nb, n " +
			"WHERE nb > 1 " +
			"SET n.ENTPersonLogin = n.ENTPersonLogin + nb ";
		queries.add(toJsonObject(loginUnicity, new JsonObject()
		.putString("nodeId", id)
		.putString("login", "ENTPersonLogin:" + login + "*")));
	}

	private void defaultOutGroupCom(String groupId) {
		String query =
				"MATCH (n:ProfileGroup)<-[:APPARTIENT]-m " +
				"WHERE n.id = {id} " +
				"CREATE UNIQUE n-[:COMMUNIQUE]->m ";
		JsonObject params = new JsonObject().putString("id", groupId);
		queriesCom.addObject(toJsonObject(query, params));
	}

	private void defaultInsideGroupCom(String groupId) {
		String query =
				"MATCH (n:ProfileGroup)<-[:APPARTIENT]-(m:User) " +
				"WHERE n.id = {id} " +
				"CREATE UNIQUE m-[:COMMUNIQUE]->n ";
		String query2 =
				"MATCH (n:ProfileGroup)<-[:APPARTIENT]-(m:User) " +
				"WHERE n.id = {id} " +
				"CREATE UNIQUE m<-[:COMMUNIQUE]-n ";
		JsonObject params = new JsonObject().putString("id", groupId);
		queriesCom
			.addObject(toJsonObject(query, params))
			.addObject(toJsonObject(query2, params));
	}

	private void defaultParentsChildsCom(JsonObject mapping) {
		String query =
				"MATCH (e:Student), (p:Relative) " +
				"WHERE e.id = {childId} AND p.id = {parentId} " +
				"CREATE UNIQUE e-[:COMMUNIQUE_DIRECT]->p";
		String query2 =
				"MATCH (e:Student), (p:Relative) " +
				"WHERE e.id = {childId} AND p.id = {parentId} " +
				"CREATE UNIQUE e<-[:COMMUNIQUE_DIRECT]-p";
		queriesCom.addObject(toJsonObject(query, mapping)).addObject(toJsonObject(query2, mapping));
	}

	private void defaultTeacherClassCom(String teacherClassGroupId) {
		String query =
				"MATCH (n:ProfileGroup)-[:DEPENDS]->(c:Class)<-[:DEPENDS]-(g:ProfileGroup) " +
				"WHERE n.id = {groupId} AND " +
				"('ClassStudentGroup' IN labels(g) OR 'ClassRelativeGroup' IN labels(g)) " +
				"CREATE UNIQUE g-[:COMMUNIQUE]->n ";
		String query2 =
				"MATCH (n:ProfileGroup)-[:DEPENDS]->(c:Class)<-[:DEPENDS]-(g:ClassRelativeGroup), (m:ProfileGroup) " +
				"WHERE n.id = {groupId} AND m.id = {groupDirectorId} " +
				"CREATE UNIQUE g-[:COMMUNIQUE]->m ";
		JsonObject params = new JsonObject()
		.putString("groupId", teacherClassGroupId);
		queriesCom.addObject(toJsonObject(query, params));
		queriesCom.addObject(toJsonObject(query2, params.copy()
				.putString("groupDirectorId",
				((JsonObject) queries.get(4)).getObject("params").getString("id"))));
	}

	private void comBetweenGroup(String outGroupId, String inGroupId) {
		String query =
				"MATCH (n:ProfileGroup), (m:ProfileGroup) " +
				"WHERE n.id = {outGroupId} AND m.id = {inGroupId} " +
				"CREATE UNIQUE n-[:COMMUNIQUE]->m ";
		JsonObject params = new JsonObject()
		.putString("outGroupId", outGroupId)
		.putString("inGroupId", inGroupId);
		queriesCom.addObject(toJsonObject(query, params));
	}

	private JsonObject toJsonObject(String query, JsonObject params) {
		return new JsonObject()
		.putString("query", query)
		.putObject("params", (params != null) ? params : new JsonObject());
	}
}
