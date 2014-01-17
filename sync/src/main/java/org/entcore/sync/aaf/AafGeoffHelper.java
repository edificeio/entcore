/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.entcore.sync.aaf;

import edu.one.core.infra.TracerHelper;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.vertx.java.core.Handler;
import org.vertx.java.core.eventbus.EventBus;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.JsonObject;

/**
 *
 * @author bperez
 */
public class AafGeoffHelper {
	private TracerHelper trace;
	private EventBus eb;
	private List<String> regroupementsCrees;
	public int total = 0;
	StringBuffer currentRequest;

	public AafGeoffHelper(TracerHelper trace, EventBus eb, WordpressHelper wph) {
		this.trace = trace;
		this.eb = eb;
		currentRequest = new StringBuffer();
		regroupementsCrees = new ArrayList<>();
	}

	public int reset() {
		currentRequest = new StringBuffer();
		regroupementsCrees = new ArrayList<>();
		int ret = total;
		total = 0;
		return ret;
	}

	public void sendRequest() {
		JsonObject jo = new JsonObject();
		jo.putString("action", "batch-insert");
		jo.putString("query", currentRequest.toString());
		final long startSend = System.currentTimeMillis();
		eb.send("wse.neo4j.persistor", jo , new Handler<Message<JsonObject>>() {
			public void handle(Message<JsonObject> m) {
				trace.info(m.body().encode());
				trace.info("Send execution time : " + (System.currentTimeMillis()-startSend) + " ms");
			}
		});
	}

	public void addOperation(Operation op) {
		currentRequest.append("(").append(op.typeEntite.toString())
				.append("_").append(AafUtils.normalizeRef(op.id)).append(")");
		currentRequest.append(" {").append("\"id\": \"").append(op.id).append("\"");
		currentRequest.append(", ").append("\"type\": \"").append(op.typeEntite).append("\"");
		// parcours de tous les attributs de l'objet
		for (Map.Entry<String, List<String>> entry : op.attributs.entrySet()) {
			// on ne traite que les attributs avec au moins une valeur
			if (entry.getValue().size() > 0) {
				currentRequest.append(", \"").append(entry.getKey()).append("\": \"");
				// parcours des valeurs associées à l'attribut
				boolean firstValue = true;
				for (String valeur : entry.getValue()) {
					if (firstValue) {
						firstValue = false;
					} else {
						// ajout d'un séparateur pour les attributs multivalués
						currentRequest.append(AafConstantes.MULTIVALUE_SEPARATOR);
					}
					currentRequest.append(valeur);
				}
				currentRequest.append("\"");
			}
		}
		currentRequest.append("}\n");
		total++;
	}

	private void addRelation(String noeud1, String noeud2, String relation) {
		currentRequest.append("(").append(noeud1).append(")");
		currentRequest.append("-[:").append(relation).append("]->");
		currentRequest.append("(").append(noeud2).append(")\n");
		total++;
	}

	public List<Operation> relGroupements(
			Operation op, Operation.TypeEntite typeEntite, String nomAttr) {
		List<Operation> groupsOperations = new ArrayList<>();
		// parcourir les groupements de l'opération
		for (String grp : op.attributs.get(nomAttr)) {
			String noeudGroupement = typeEntite.toString() + "_"+ AafUtils.normalizeRef(grp);
			// créer le groupement s'il n'existe pas
			if (!regroupementsCrees.contains(grp)) {
				Map<String,List<String>> attrs = getAttrsRegroupement(grp);
				if (!attrs.isEmpty()) {
					Operation opGr = new Operation(Operation.TypeOperation.ADDREQUEST.toString());
					opGr.id = grp;
					opGr.typeEntite = typeEntite;
					opGr.attributs = attrs;
					groupsOperations.add(opGr);
					addOperation(opGr);
					// création de la relation groupement / école
					String noeudEcole = Operation.TypeEntite.ETABEDUCNAT.toString()
							+ "_" + attrs.get(AafConstantes.GROUPE_ECOLE_ATTR).get(0);
					addRelation(noeudGroupement,noeudEcole,AafConstantes.REL_APPARTIENT);
				}
			}
			// création du lien groupement / utilisateur
			String noeudPersonne = op.typeEntite.toString() + "_" + op.id;
			addRelation(noeudPersonne,noeudGroupement,AafConstantes.REL_APPARTIENT);
		}
		return groupsOperations;
	}

	public void relEcoles(Operation op) {
		// parcourir les écoles de l'opération
		for (String ecole : op.attributs.get(AafConstantes.ECOLE_ATTR)) {
			// création du lien école / utilisateur
			String noeudEcole = Operation.TypeEntite.ETABEDUCNAT.toString() + "_" + ecole;
			String noeudPersonne = op.typeEntite.toString() + "_" + op.id;
			addRelation(noeudPersonne,noeudEcole,AafConstantes.REL_APPARTIENT);
		}
	}

	public void relParents(Operation op) {
		// parcourir les parents de l'opération
		for (String parent : op.attributs.get(AafConstantes.PARENTS_ATTR)) {
			String[] parentAttr = parent.split(AafConstantes.AAF_SEPARATOR);
			// création du lien parent / enfant
			String noeudEnfant = op.typeEntite.toString() + "_" + op.id;
			String noeudParent = Operation.TypeEntite.PERSRELELEVE.toString()
					+ "_" + parentAttr[AafConstantes.PARENT_ID_INDEX];
			addRelation(noeudEnfant,noeudParent,AafConstantes.REL_PARENT);
		}
	}

	private Map<String,List<String>> getAttrsRegroupement(String regroupement) {
		Map<String,List<String>> attrs = new HashMap<>();
		String[] values = regroupement.split(AafConstantes.AAF_SEPARATOR);
		// 3 valeurs normalement : idEcole + typeGroupe + nomGroupe
		if (values.length == 3) {
			attrs.put(AafConstantes.GROUPE_ECOLE_ATTR
					, Arrays.asList(values[AafConstantes.GROUPE_ECOLE_INDEX]));
			attrs.put(AafConstantes.GROUPE_TYPE_ATTR
					, Arrays.asList(values[AafConstantes.GROUPE_TYPE_INDEX]));
			attrs.put(AafConstantes.GROUPE_NOM_ATTR
					, Arrays.asList(values[AafConstantes.GROUPE_NOM_INDEX]));
		} else {
			trace.error("Groupement non conforme : " + regroupement + " => " + values.length);
		}
		return attrs;
	}
}