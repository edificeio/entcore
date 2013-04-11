package edu.one.core.datadictionary.dictionary;

import edu.one.core.datadictionary.dictionary.aaf.AAFField;
import edu.one.core.datadictionary.validation.RegExpValidator;
import edu.one.core.datadictionary.validation.Validator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.logging.Logger;
import org.vertx.java.platform.Container;

public class DefaultDictionary implements Dictionary {

	protected Logger logger;

	protected Map<String, Field> users;
	protected Map<String, Field> groups;
	protected Map<String, Field> structures;

	protected Map<String, Validator> validators;

	public DefaultDictionary(Vertx vertx, Container container, String dictionnaryFileName) {
		logger = container.getLogger();
		try {
			validators = new HashMap<>();
			for (String regexpKey : RegExpValidator.types.keySet()) {
				validators.put(regexpKey, RegExpValidator.instance(regexpKey));
			}
			// Null validator return always true
			validators.put(null, new Validator() {
				public boolean test(String s) {
					return true;
				}
			});

			users = new HashMap<>();
			groups = new HashMap<>();
			structures = new HashMap<>();

			JsonObject jo = new JsonObject(vertx.fileSystem().readFileSync(dictionnaryFileName).toString());

			// TODO : revoir les responsabilités de construction
			for (Object o : jo.getArray("personnes")) {
				AAFField aAFField = new AAFField((JsonObject)o);
				users.put(aAFField.name, aAFField);
			}

		} catch (Exception ex) {
			logger.error(ex.getMessage());
		}
	}


	@Override
	public boolean validateField(String name, List<String> values) {
		Field f = users.get(name);
		if (f == null) {
			return false;
		}
		for (String value : values) {
			if (!validators.get(f.validator).test(value)) {
				return false;
			}
		}
		return true;
	}

	@Override
	public Map<String, Boolean> validateFields(Map<String, List<String>> fields) {
		Map<String, Boolean> result = new HashMap<>();
		for (Map.Entry<String, List<String>> entry : fields.entrySet()) {
			result.put(entry.getKey(), validateField(entry.getKey(), entry.getValue()));
		}
		return result;
	}

	@Override
	public Field getField(String name) {
		return users.get(name);
	}

	@Override
	public void link(String oneField, String specificField) {
		throw new UnsupportedOperationException("Not supported yet.");
	}
}
