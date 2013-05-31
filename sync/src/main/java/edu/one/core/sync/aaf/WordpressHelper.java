/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.one.core.sync.aaf;

import edu.one.core.infra.TracerHelper;
import java.util.HashMap;
import java.util.Map;
import org.vertx.java.core.eventbus.EventBus;
import org.vertx.java.core.json.JsonObject;

/**
 *
 * @author bperez
 */
public class WordpressHelper {
	private TracerHelper trace;
	private EventBus eb;
	private Map<String,Map<String,String>> schools;
	private Map<String,Map<String,String>> persons;
	private Map<String,Map<String,String>> groups;
	private String ID_ATTR = "id";
	private String TYPE_ATTR = "type";
	private String NAME_ATTR = "nom";
	private String SURNAME_ATTR = "prenom";
	private String LOGIN_ATTR = "login";
	private String PASSWORD_ATTR = "password";
	private String SCHOOL_ATTR = "ecole";
	private String CLASS_ATTR = "classe";

	public WordpressHelper(TracerHelper trace, EventBus eb) {
		this.trace = trace;
		this.eb = eb;
		schools = new HashMap<>();
		persons = new HashMap<>();
		groups = new HashMap<>();
	}

	public void reset() {
		schools = new HashMap<>();
		persons = new HashMap<>();
		groups = new HashMap<>();
	}

	public void addEntity(Operation op) {
		// TODO : contrôles existence, not null, etc.
		switch (op.typeEntite) {
			case ETABEDUCNAT :
				addSchool(op.id
						, op.typeEntite.toString()
						, op.attributs.get(AafConstantes.STRUCTURE_NOM_ATTR).get(0));
				break;
			case CLASSE :
				addGroup(AafUtils.normalizeRef(op.id)
						, op.typeEntite.toString()
						, op.attributs.get(AafConstantes.GROUPE_NOM_ATTR).get(0)
						, op.attributs.get(AafConstantes.GROUPE_ECOLE_ATTR).get(0));
				break;
			case GROUPE :
				addGroup(AafUtils.normalizeRef(op.id)
						, op.typeEntite.toString()
						, op.attributs.get(AafConstantes.GROUPE_NOM_ATTR).get(0)
						, op.attributs.get(AafConstantes.GROUPE_ECOLE_ATTR).get(0));
				break;
			case PERSEDUCNAT :
				addPerson(op.id
						, op.typeEntite.toString()
						, op.attributs.get(AafConstantes.PERSONNE_NOM_ATTR).get(0)
						, op.attributs.get(AafConstantes.PERSONNE_PRENOM_ATTR).get(0)
						, op.attributs.get(AafConstantes.PERSONNE_LOGIN_ATTR).get(0)
						, op.attributs.get(AafConstantes.PERSONNE_PASSWORD_ATTR).get(0));
				addPersonClass(
						op.id, AafUtils.normalizeRef(op.attributs.get(AafConstantes.CLASSES_ATTR).get(0)));
				break;
			case ELEVE :
				addPerson(op.id
						, op.typeEntite.toString()
						, op.attributs.get(AafConstantes.PERSONNE_NOM_ATTR).get(0)
						, op.attributs.get(AafConstantes.PERSONNE_PRENOM_ATTR).get(0)
						, op.attributs.get(AafConstantes.PERSONNE_LOGIN_ATTR).get(0)
						, op.attributs.get(AafConstantes.PERSONNE_PASSWORD_ATTR).get(0));
				addPersonClass(
						op.id, AafUtils.normalizeRef(op.attributs.get(AafConstantes.CLASSES_ATTR).get(0)));
				// liens parent / classe
				for (String parent : op.attributs.get(AafConstantes.PARENTS_ATTR)) {
					String[] parentAttr = parent.split(AafConstantes.AAF_SEPARATOR);
					addPersonClass(parentAttr[AafConstantes.PARENT_ID_INDEX]
							, AafUtils.normalizeRef(op.attributs.get(AafConstantes.CLASSES_ATTR).get(0)));
				}
				break;
			case PERSRELELEVE :
				addPerson(op.id
						, op.typeEntite.toString()
						, op.attributs.get(AafConstantes.PERSONNE_NOM_ATTR).get(0)
						, op.attributs.get(AafConstantes.PERSONNE_PRENOM_ATTR).get(0)
						, op.attributs.get(AafConstantes.PERSONNE_LOGIN_ATTR).get(0)
						, op.attributs.get(AafConstantes.PERSONNE_PASSWORD_ATTR).get(0));
				break;
			default :
				break;
		}
	}

	private void addSchool(String id, String type, String name) {
		Map<String,String> attrs = new HashMap<>();
		attrs.put(ID_ATTR, id);
		attrs.put(TYPE_ATTR, type);
		attrs.put(NAME_ATTR, name);
		schools.put(id, attrs);
	}

	private void addGroup(String id, String type, String name, String school) {
		Map<String,String> attrs = new HashMap<>();
		attrs.put(ID_ATTR, id);
		attrs.put(TYPE_ATTR, type);
		attrs.put(NAME_ATTR, name);
		attrs.put(SCHOOL_ATTR, school);
		groups.put(id, attrs);
	}

	private void addPerson(
			String id, String type, String name, String surname, String login, String password) {
		Map<String,String> attrs = new HashMap<>();
		attrs.put(ID_ATTR, id);
		attrs.put(TYPE_ATTR, type);
		attrs.put(NAME_ATTR, name);
		attrs.put(SURNAME_ATTR, surname);
		attrs.put(LOGIN_ATTR, login);
		attrs.put(PASSWORD_ATTR, password);
		persons.put(id, attrs);
	}

	private void addPersonClass(String idPerson, String personClass) {
		// TODO : gestion multiclasses
		if (persons.containsKey(idPerson) && !persons.get(idPerson).containsKey(CLASS_ATTR)) {
				persons.get(idPerson).put(CLASS_ATTR, personClass);
		}
	}

	public void send() throws InterruptedException {
		for (Map.Entry<String, Map<String, String>> entry : schools.entrySet()) {
			sendWP(entry.getValue());
		}
		Thread.sleep(1000);
		for (Map.Entry<String, Map<String, String>> entry : groups.entrySet()) {
			sendWP(entry.getValue());
		}
		Thread.sleep(1000);
		for (Map.Entry<String, Map<String, String>> entry : persons.entrySet()) {
			sendWP(entry.getValue());
		}
	}

	private void sendWP(Map<String,String> attrs) {
		JsonObject jo = new JsonObject();
		for (Map.Entry<String, String> entry : attrs.entrySet()) {
			jo.putString(entry.getKey(), entry.getValue());
		}
		eb.send("wpconnector.address", jo);
	}
}