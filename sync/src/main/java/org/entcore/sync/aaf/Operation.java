package org.entcore.sync.aaf;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 *
 * @author bperez
 */
public class Operation {
	public enum TypeOperation {
		ADDREQUEST,MODIFYREQUEST,DELETEREQUEST;
	};
	public TypeOperation typeOperation;
	public enum TypeEntite {
		ETABEDUCNAT,ELEVE,PERSRELELEVE,PERSEDUCNAT,CLASSE,GROUPE;
	};
	public TypeEntite typeEntite;
	public String id;
	public Map<String,List<String>> attributs;
	public Map.Entry<String,List<String>> attributCourant;
	public enum EtatAvancement {
		TYPE_OPERATION,TYPE_ENTITE,ID,ATTRIBUTS;

		public EtatAvancement suivant() {
			switch (this) {
					case TYPE_OPERATION :
						return TYPE_ENTITE;
					case TYPE_ENTITE :
						return ID;
					case ID :
						return ATTRIBUTS;
					default :
						return null;
			}
		}
	};
	public EtatAvancement etatAvancement;
	public Operation(String typeOperation) {
		etatAvancement = EtatAvancement.TYPE_OPERATION;
		this.typeOperation = TypeOperation.valueOf(typeOperation.toUpperCase());
		attributs = new HashMap<>();
	}

	public void creerAttributCourant(String attributNom) {
		attributCourant = new AbstractMap.SimpleEntry(attributNom, new ArrayList<String>());
	}

	public void ajouterValeur(String valeur) {
		attributCourant.getValue().add(valeur);
	}

	public void ajouterAttributCourant() {
		attributs.put(attributCourant.getKey(), attributCourant.getValue());
	}
}