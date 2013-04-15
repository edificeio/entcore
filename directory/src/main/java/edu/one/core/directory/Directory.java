package edu.one.core.directory;

import edu.one.core.datadictionary.dictionary.DefaultDictionary;
import edu.one.core.infra.Controller;
import edu.one.core.infra.Neo;
import java.util.Map;
import org.vertx.java.core.Handler;
import org.vertx.java.core.http.HttpServerRequest;
import org.vertx.java.core.json.JsonObject;
import edu.one.core.datadictionary.dictionary.Dictionary;
import java.util.ArrayList;
import java.util.List;

public class Directory extends Controller {
	
	JsonObject dataMock;
	Neo neo;
	Dictionary d;

	@Override
	public void start() throws Exception {
		super.start();
		neo = new Neo(vertx.eventBus(),log);
		d = new DefaultDictionary(vertx, container, "../edu.one.core~dataDictionary~0.1.0-SNAPSHOT/aaf-dictionary.json");
		dataMock = new JsonObject(vertx.fileSystem().readFileSync("directory-data-mock.json").toString());
		container.deployModule("edu.one~wordpress~1.0.0-SNAPSHOT");

		rm.get("/admin", new Handler<HttpServerRequest>() {
			@Override
			public void handle(HttpServerRequest request) {
				renderView(request, new JsonObject());
			}
		});
		
		rm.get("/admin-export", new Handler<HttpServerRequest>() {
			@Override
			public void handle(HttpServerRequest request) {
				renderView(request, new JsonObject());
			}
		});
		
		rm.get("/admin-create", new Handler<HttpServerRequest>() {
			@Override
			public void handle(HttpServerRequest request) {
				renderView(request, new JsonObject());
			}
		});

		rm.get("/api/ecole", new Handler<HttpServerRequest>() {
			@Override
			public void handle(HttpServerRequest request) {
				neo.send("START n=node(*) WHERE has(n.type) "
						+ "AND n.type='ETABEDUCNAT' "
						+ "RETURN distinct n.ENTStructureNomCourant, n.id", request.response);
			}
		});
		
		rm.get("/api/groupes", new Handler<HttpServerRequest>() {
			@Override
			public void handle(HttpServerRequest request) {
				neo.send("START n=node(*) WHERE has(n.type) "
						+ "AND n.type='GROUPE' "
						+ "RETURN distinct n.ENTGroupeNom, n.id, n.ENTPeople", request.response);
			}
		});

		rm.get("/api/classes", new Handler<HttpServerRequest>() {
			@Override
			public void handle(HttpServerRequest request) {
				String schoolId = request.params().get("id");
				neo.send("START n=node(*) MATCH n<--m WHERE has(n.id) "
						+ "AND n.id='" + schoolId + "' "
						+ "AND has(m.type) AND m.type='CLASSE' "
						+ "RETURN distinct m.ENTGroupeNom, m.id, n.id", request.response);
			}
		});

		rm.get("/api/personnes", new Handler<HttpServerRequest>() {
			@Override
			public void handle(HttpServerRequest request) {
				String classId = request.params().get("id").replaceAll("-", " ").replaceAll("_", "\\$");
				neo.send("START n=node(*) MATCH n<--m WHERE has(n.id) "
						+ "AND n.id='" + classId + "' "
						+ "AND has(m.type) AND (m.type='ELEVE' OR m.type='PERSRELELEVE' OR m.type='PERSEDUCNAT') "
						+ "RETURN distinct m.id,m.ENTPersonNom,m.ENTPersonPrenom, n.id", request.response);
			}
		});

		rm.get("/api/membres", new Handler<HttpServerRequest>() {
			@Override
			public void handle(HttpServerRequest request) {
				String[] people = request.params().get("data").split("-");
				String neoRequest = "START n=node(*) where has(n.id) and (";
				for (String id : people) {
					neoRequest += "n.id='" + id + "' or ";
				}
				neoRequest += "n.id='') return distinct n.id, n.ENTPersonNom, n.ENTPersonPrenom";
				neo.send(neoRequest, request.response);
			}
		});

		rm.get("/api/details", new Handler<HttpServerRequest>() {
			@Override
			public void handle(HttpServerRequest request) {
				String personId = request.params().get("id");
				neo.send("START n=node(*) WHERE has(n.id) "
						+ "AND n.id='" + personId + "' "
						+ "RETURN distinct n.ENTPersonNom, n.ENTPersonPrenom, n.ENTPersonAdresse", request.response);
			}
		});

		rm.get("/api/enseignants", new Handler<HttpServerRequest>(){
			@Override
			public void handle(HttpServerRequest request) {
				String classId = request.params().get("id").replaceAll("-", " ").replaceAll("_", "\\$");
				System.out.println("PARAM :" + classId);
				neo.send("START n=node(*), m=node(*) WHERE has(m.type) "
						+ "AND m.type='PERSEDUCNAT' AND has(n.id) "
						+ "AND n.id='" + classId + "' "
						+ "RETURN distinct m.id, m.ENTPersonNom, m.ENTPersonPrenom, n.id", request.response);
			}
		});

		rm.get("/api/create-user", new Handler<HttpServerRequest>(){
			@Override
			public void handle(HttpServerRequest request) {
				JsonObject obj = new JsonObject();
				Map<String,Boolean> params = d.validateFields(request.params());
				if (!params.values().contains(false)){
					trace.info("Creating new User : " + request.params().get("ENTPersonNom") + " " + request.params().get("ENTPersonPrenom"));
					neo.send("START n=node(*) WHERE has(n.ENTGroupeNom) "
							+ "AND n.ENTGroupeNom='" + request.params().get("ENTPersonStructRattach").replaceAll("-", " ") + "' "
							+ "CREATE (m {id:'m0000001', type:'" + request.params().get("ENTPersonProfils") + "',"
							+ "ENTPersonNom:'"+request.params().get("ENTPersonNom") +"', "
							+ "ENTPersonPrenom:'"+request.params().get("ENTPersonPrenom") +"', "
							+ "ENTPersonDateNaissance:'"+request.params().get("ENTPersonDateNaissance") +"'}), "
							+ "m-[:APPARTIENT]->n ", request.response);
				} else {
					obj.putString("result", "error");
					for (Map.Entry<String, Boolean> entry : params.entrySet()) {
						if (!entry.getValue()){
							obj.putBoolean(entry.getKey(), entry.getValue());
						}
					}
					renderJson(request, obj);
				}
			}
		});

		rm.get("/api/create-group", new Handler<HttpServerRequest>(){
			@Override
			public void handle(HttpServerRequest request) {
				List users = new ArrayList<String>();
				for (Map.Entry<String, String> entry : request.params().entrySet()) {
					if (!entry.getKey().equals("ENTGroupeNom")){
						users.add(entry.getValue());
					}
				}
				trace.info("Creating new Group : " + request.params().get("ENTGroupeNom"));
				neo.send("START n=node(*) WHERE has(n.id) AND n.id='4400000002'"
						+ "CREATE (m {id:'g0000001', type:'GROUPE',"
						+ "ENTGroupeNom:'"+request.params().get("ENTGroupeNom")
						+"', ENTPeople:'" + users.toString() + "'}), "
						+ "m-[:APPARTIENT]->n ", request.response);
			}
		});

		rm.get("/api/export", new Handler<HttpServerRequest>() {
			@Override
			public void handle(HttpServerRequest request) {
				String neoRequest = createExportRequest(request.params());
				trace.info("Exporting auth data for " + request.params().get("id"));
				neo.send(neoRequest, request.response);
			}
		});

	}


	private String createExportRequest(Map<String,String> params){
		if (params.get("id").equals("all")){
			return "START m=node(*) WHERE has(m.type) "
					+ "AND (m.type='ELEVE' OR m.type='PERSEDUCNAT' OR m.type='PERSRELELEVE') "
					+ "RETURN distinct m.id,m.ENTPersonNom, m.ENTPersonPrenom";
		} else {
			return "START n=node(*) MATCH n<--m "
					+ "WHERE has(n.id) AND n.id='" + params.get("id") + "' "
					+ "AND has(m.type) AND (m.type='ELEVE' OR m.type='PERSEDUCNAT' OR m.type='PERSRELELEVE') "
					+ "RETURN distinct m.id,m.ENTPersonNom,m.ENTPersonPrenom";
		}
	}

}