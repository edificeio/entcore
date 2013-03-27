package edu.one.core.sync.aaf;

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
		ETABEDUCNAT,ELEVE,PERSRELELEVE,PERSEDUCNAT;
	};
	public TypeEntite typeEntite;
	public String id;
	public StringBuffer attributsBuf;
	public StringBuffer attributCourantBuf;
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
		attributsBuf = new StringBuffer();
	}

	public void creerAttributCourant(String attributNom) {
		attributCourantBuf = new StringBuffer();
		attributCourantBuf.append(attributNom).append(AafConstantes.VALUE_SEPARATOR);
	}

	public void ajouterValeur(String valeur) {
		attributCourantBuf.append(valeur).append(AafConstantes.MULTIVALUE_SEPARATOR);
	}

	public void ajouterAttributCourant() {
		attributsBuf.append(attributCourantBuf.toString()).append(AafConstantes.ATTR_SEPARATOR);
	}

//	public String requeteCreation(String init) {
//		String requete = init;
//		for (Map.Entry<String, List<String>> entry : attributs.entrySet()) {
//			String nomAttribut = entry.getKey();
//			List<String> valeursAttribut = entry.getValue();
//
//			if (valeursAttribut.size() > 0) {
//				if (requete.equals(init)) {
//					requete += "({";
//					requete += nomAttribut + " : '"
//							+ valeursAttribut.get(0).replaceAll("'", "\\\\'") + "'";
//				} else {
//					requete += ", " + nomAttribut + " : '"
//							+ valeursAttribut.get(0).replaceAll("'", "\\\\'") + "'";
//				}
//			}
//		}
//		requete += "})";
//		return requete;
//	}

//	@Override
//	public String toString() {
//		String str = "";
//		for (Map.Entry<String, List<String>> entry : attributs.entrySet()) {
//			List<String> valeursAttribut = entry.getValue();
//			if (valeursAttribut.size() > 0) {
////				if (str.equals("")) {
////					str = entry.getKey() + AafConstantes.VALUE_SEPARATOR + entry.getValue().get(0);
////				} else {
//					str = str + AafConstantes.ATTR_SEPARATOR + entry.getKey()
//							+ AafConstantes.VALUE_SEPARATOR + entry.getValue().get(0);
////				}
//			}
//		}
//		return str;
//	}
}