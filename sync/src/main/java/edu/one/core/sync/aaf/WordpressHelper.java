/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.one.core.sync.aaf;

import java.util.HashMap;
import java.util.Map;
import org.vertx.java.core.eventbus.EventBus;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.logging.Logger;

/**
 *
 * @author bperez
 */
public class WordpressHelper {
	private Logger log;
	private EventBus eb;
	private Map<String,Map<String,String>> schools;
	private Map<String,Map<String,String>> persons;
	private Map<String,Map<String,String>> groups;
	private String ID_ATTR = "id";
	private String TYPE_ATTR = "type";
	private String NAME_ATTR = "nom";
	private String SURNAME_ATTR = "prenom";
	private String SCHOOL_ATTR = "ecole";
	private String CLASS_ATTR = "classe";
	
	public WordpressHelper(Logger log, EventBus eb) {
		this.log = log;
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
	
	public void addSchool(String id, String type, String name) {
		Map<String,String> attrs = new HashMap<>();
		attrs.put(ID_ATTR, id);
		attrs.put(TYPE_ATTR, type);
		attrs.put(NAME_ATTR, name);
		schools.put(id, attrs);
	}
	
	public void addGroup(String id, String type, String name, String school) {
		Map<String,String> attrs = new HashMap<>();
		attrs.put(ID_ATTR, id);
		attrs.put(TYPE_ATTR, type);
		attrs.put(NAME_ATTR, name);
		attrs.put(SCHOOL_ATTR, school);
		groups.put(id, attrs);
	}
	
	public void addPerson(String id, String type, String name, String surname) {
		Map<String,String> attrs = new HashMap<>();
		attrs.put(ID_ATTR, id);
		attrs.put(TYPE_ATTR, type);
		attrs.put(NAME_ATTR, name);
		attrs.put(SURNAME_ATTR, surname);
		persons.put(id, attrs);
	}
	
	public void addPersonClass(String idPerson, String personClass) {
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