package edu.one.core.datadictionary.dictionary.aaf;

import edu.one.core.datadictionary.dictionary.Category;
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

	public AAFField(Category<String, Field> c, JsonObject jo) throws Exception{
		parent = c;
		id = jo.getString("id");
		label = jo.getString("label");
		note = jo.getString("note");
		isRequired = jo.getBoolean("obligatoire", false);
		isMultiple = jo.getBoolean("multiple", false);
		validator = jo.getString("validator");
		isEditable = jo.getBoolean("modifiable", true);

		if (jo.getArray("restrictions") != null) {
			for (Object o : jo.getArray("restrictions")) {
				if (!parent.getTypes().contains((String)o)) {
					throw new Exception("Restrictions of field :" + id + "are not valid");
				}
			}
			setRestrictions(jo.getArray("restrictions").toArray());
		}
	}

}
