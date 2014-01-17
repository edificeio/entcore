package org.entcore.datadictionary.dictionary.aaf;

import org.entcore.datadictionary.dictionary.Category;
import org.entcore.datadictionary.dictionary.Field;
import java.util.ArrayList;
import java.util.List;
import org.vertx.java.core.json.JsonObject;

public class AAFField extends Field {

	public AAFField(Category  c, JsonObject jo) throws Exception {
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
					throw new Exception("Restrictions of field :" + id + " are not valid");
				}
			}
			setRestrictions(jo.getArray("restrictions").toArray());
		}

		if (jo.getObject("auto") != null) {
			List<String> l = new ArrayList<>();
			for (Object fieldId : jo.getObject("auto").getArray("args").toArray()) {
				l.add((String)fieldId);
			}
			generator = AAFGenerators.instance(jo.getObject("auto").getString("generator"), l.toArray(new String[]{}));
		}

	}

}
