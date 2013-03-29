/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.one.core.sync.aaf;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.vertx.java.core.Handler;
import org.vertx.java.core.eventbus.EventBus;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.logging.Logger;

/**
 *
 * @author bperez
 */
public class AafGeoffHelper {
	private Logger log;
	private EventBus eb;
	private List<String> regroupementsCrees;
	public int total = 0;
	
	public AafGeoffHelper(Logger log, EventBus eb) {
		this.log = log;
		this.eb = eb;
		regroupementsCrees = new ArrayList<>();
	}

	public int reset() {
		regroupementsCrees = new ArrayList<>();
		int ret = total;
		total = 0;
		return ret;
	}

	public void sendRequest(List<Operation> operations) {
		StringBuffer request = new StringBuffer();
		// parcours de toutes les opérations à effectuer pour l'import
		for (Operation operation : operations) {
			// création du noeud associé à l'opération avec ses attributs
			addRequest(request, operation.id, operation.typeEntite.toString(), operation.attributs);
			
			// création des relations et des groupements éventuels
			if (operation.typeEntite == Operation.TypeEntite.ELEVE
					|| operation.typeEntite == Operation.TypeEntite.PERSEDUCNAT) {
				// TODO : refactorer?
				if (operation.attributs.containsKey(AafConstantes.CLASSES_ATTR)) {
					relGroupements(
							request,operation,Operation.TypeEntite.CLASSE,AafConstantes.CLASSES_ATTR);
				}
				if (operation.attributs.containsKey(AafConstantes.GROUPES_ATTR)) {
					relGroupements(
							request,operation,Operation.TypeEntite.GROUPE,AafConstantes.GROUPES_ATTR);
				}
				if (operation.attributs.containsKey(AafConstantes.ECOLE_ATTR)) {
					relEcoles(request,operation);
				}
				if (operation.typeEntite == Operation.TypeEntite.ELEVE &&
						operation.attributs.containsKey(AafConstantes.PARENTS_ATTR)) {
					relParents(request,operation);
				}
			}
		}
//		log.info(request);
		// TODO : envoyer requete via plugin geoff
		JsonObject jo = new JsonObject();
		jo.putString("action", "batch-insert");
		jo.putString("query", request.toString());
		eb.send("wse.neo4j.persistor", jo , new Handler<Message<JsonObject>>() {
			public void handle(Message<JsonObject> m) {
				log.info(m.body.encode());
			}
		});
		eb.send("wse.neo4j.persistor", jo);
	}

	private void addRequest(
			StringBuffer req, String id, String typeEntite, Map<String,List<String>> attrs) {
		req.append("(").append(typeEntite).append("_").append(normalizeRef(id)).append(")");
		req.append(" {").append("\"id\": \"").append(id).append("\"");
		req.append(", ").append("\"type\": \"").append(typeEntite).append("\"");
		// parcours de tous les attributs de l'objet
		for (Map.Entry<String, List<String>> entry : attrs.entrySet()) {
			// on ne traite que les attributs avec une valeur
			if (entry.getValue().size() > 0) {
				req.append(", \"").append(entry.getKey()).append("\": \"");
				// parcours des valeurs associées à l'attribut
				boolean firstValue = true;
				for (String valeur : entry.getValue()) {
					if (firstValue) {
						firstValue = false;
					} else {
						// ajout d'un séparateur pour les attributs multivalués
						req.append(AafConstantes.MULTIVALUE_SEPARATOR);
					}
					req.append(valeur);
				}
				req.append("\"");
			}
		}
		req.append("}\n");
		total++;
	}

	private void addRelation(StringBuffer req, String noeud1, String noeud2, String relation) {
		req.append("(").append(noeud1).append(")");
		req.append("-[:").append(relation).append("]->");
		req.append("(").append(noeud2).append(")\n");
		total++;
	}
	
	private void relGroupements(
			StringBuffer req, Operation op, Operation.TypeEntite typeEntite, String nomAttr) {
		// parcourir les groupements de l'opération
		for (String grp : op.attributs.get(nomAttr)) {
			String noeudGroupement = typeEntite.toString() + "_"+ normalizeRef(grp);
			// créer le groupement s'il n'existe pas
			if (!regroupementsCrees.contains(grp)) {
				Map<String,List<String>> attrs = getAttrsRegroupement(grp);
				if (!attrs.isEmpty()) {
					addRequest(req, grp, typeEntite.toString(), attrs);
					// création de la relation groupement / école
					String noeudEcole = Operation.TypeEntite.ETABEDUCNAT.toString()
							+ "_" + attrs.get(AafConstantes.GROUPE_ECOLE_ATTR).get(0);
					addRelation(req,noeudGroupement,noeudEcole,AafConstantes.REL_APPARTIENT);
				}
			}
			// création du lien groupement / utilisateur
			String noeudPersonne = op.typeEntite.toString() + "_" + op.id;
			addRelation(req,noeudPersonne,noeudGroupement,AafConstantes.REL_APPARTIENT);
		}
	}
	
	private void relEcoles(StringBuffer req, Operation op) {
		// parcourir les écoles de l'opération
		for (String ecole : op.attributs.get(AafConstantes.ECOLE_ATTR)) {
			// création du lien école / utilisateur
			String noeudEcole = Operation.TypeEntite.ETABEDUCNAT.toString() + "_" + ecole;
			String noeudPersonne = op.typeEntite.toString() + "_" + op.id;
			addRelation(req,noeudPersonne,noeudEcole,AafConstantes.REL_APPARTIENT);
		}
	}
	
	private void relParents(StringBuffer req, Operation op) {
		// parcourir les parents de l'opération
		for (String parent : op.attributs.get(AafConstantes.PARENTS_ATTR)) {
			String[] parentAttr = parent.split(AafConstantes.AAF_SEPARATOR);
			// création du lien parent / enfant
			String noeudEnfant = op.typeEntite.toString() + "_" + op.id;
			String noeudParent = Operation.TypeEntite.PERSRELELEVE.toString() 
					+ "_" + parentAttr[AafConstantes.PARENT_ID_INDEX];
			addRelation(req,noeudEnfant,noeudParent,AafConstantes.REL_PARENT);
		}
	}
	
	private Map<String,List<String>> getAttrsRegroupement(String regroupement) {
		Map<String,List<String>> attrs = new HashMap<>();
		String[] values = regroupement.split(AafConstantes.AAF_SEPARATOR);
		// 3 valeurs normalement : idEcole + typeGroupe + nomGroupe
		if (values.length == 3) {
			attrs.put(
					AafConstantes.GROUPE_ECOLE_ATTR, strToList(values[AafConstantes.GROUPE_ECOLE_INDEX]));
			attrs.put(
					AafConstantes.GROUPE_TYPE_ATTR, strToList(values[AafConstantes.GROUPE_TYPE_INDEX]));
			attrs.put(
					AafConstantes.GROUPE_NOM_ATTR, strToList(values[AafConstantes.GROUPE_NOM_INDEX]));
		} else {
			log.error("Groupement non conforme : " + regroupement + " => " + values.length);
		}
		return attrs;
	}
	
	private List<String> strToList(String chaine) {
		String[] array = {chaine};
		return Arrays.asList(array);
	}
	
	private String normalizeRef(String name) {
		return name.replaceAll(AafConstantes.AAF_SEPARATOR, "_").replaceAll(" ", "");
	}
}