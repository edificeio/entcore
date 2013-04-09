package edu.one.core.datadictionary.dictionary.aaf;

import edu.one.core.datadictionary.dictionary.Field;
import org.vertx.java.core.json.JsonObject;

/*
 * Catégorie d'objet:
 * ENTEleve, ENTPersRelEleve, ENTEnseignant, ENTNonEnsEcole, ENTNonEnsServAc, ENTNonEnsCollLoc,
 * ENTPersExt, ENTEcole , ENTServAc , ENTCollLoc, ENTClasse, ENTGroupeSpecifique, ENTGroupementEcoles
 *
 * étudier l'option de migrer les constante sync*AAF* dans ce package
 */
public class AAFField extends Field {

	public AAFField(JsonObject jo) {
		this.name = jo.getString("label");
		this.label = jo.getString("attr");
		this.note = jo.getString("note");
		this.isRequired = "Mo".equals(jo.getString("required"));
		this.isMultiple = "Obl".equals(jo.getString("multiple"));
		this.validator = jo.getString("validator");
	}

}
