package edu.one.core.sync.aaf;

import edu.one.core.sync.SyncUtils;
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

	public void ajouterAttributsCalcules() {
		if (typeEntite == TypeEntite.ETABEDUCNAT) {
			// TODO : construire l'id pour s'assurer de son unicité
			attributs.put(AafConstantes.STRUCTURE_ID_ATTR, SyncUtils.strToList(id));
		} else if (typeEntite == TypeEntite.CLASSE || typeEntite == TypeEntite.GROUPE) {
			// TODO : construire l'id pour s'assurer de son unicité
			attributs.put(AafConstantes.GROUPE_ID_ATTR, SyncUtils.strToList(id));
		} else {
			// TODO : construire l'id selon les règles définies dans le SDET
			attributs.put(AafConstantes.PERSONNE_ID_ATTR, SyncUtils.strToList(id));
			
			String prenom = "";
			String nom = "";
			String civilite = "";
			if (attributs.containsKey(AafConstantes.PERSONNE_PRENOM_ATTR)) {
				prenom = attributs.get(AafConstantes.PERSONNE_PRENOM_ATTR).get(0);
			}
			if (attributs.containsKey(AafConstantes.PERSONNE_NOM_ATTR)) {
				nom = attributs.get(AafConstantes.PERSONNE_NOM_ATTR).get(0);
			}
			if (attributs.containsKey(AafConstantes.PERSONNE_CIVILITE_ATTR)) {
				civilite = attributs.get(AafConstantes.PERSONNE_CIVILITE_ATTR).get(0);
			}
					
			// TODO : check unicité login
			attributs.put(AafConstantes.PERSONNE_LOGIN_ATTR
					, SyncUtils.strToList(SyncUtils.generateLogin(prenom, nom)));
			attributs.put(AafConstantes.PERSONNE_NOM_AFFICHAGE_ATTR
					, SyncUtils.strToList(SyncUtils.generateDisplayName(prenom, nom)));
			attributs.put(AafConstantes.PERSONNE_SEXE_ATTR
					, SyncUtils.strToList(SyncUtils.generateSex(civilite)));
			attributs.put(AafConstantes.PERSONNE_PASSWORD_ATTR
					, SyncUtils.strToList(SyncUtils.generatePassword()));
		}
	}
}