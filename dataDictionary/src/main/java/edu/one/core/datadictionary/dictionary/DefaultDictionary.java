package edu.one.core.datadictionary.dictionary;

import edu.one.core.datadictionary.dictionary.aaf.AAFField;
import edu.one.core.datadictionary.validation.NoValidator;
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

	protected Category<String, Field> users;
	protected Category<String, Field> groups;
	protected Category<String, Field> structures;

	protected Map<String, Validator> validators;
	private Field defaultField = new Field();

	public DefaultDictionary(Vertx vertx, Container container, String file) {
		logger = container.logger();
		try {
			validators = RegExpValidator.all();
			validators.put(null, new NoValidator(true)); // If no validator return true

			JsonObject d = new JsonObject(vertx.fileSystem().readFileSync(file).toString());
			users = loadCategory(d, "personnes");
			groups = loadCategory(d, "groupes");
			structures = loadCategory(d, "structures");

		} catch (Exception ex) {
			ex.printStackTrace();
			logger.error(ex.getMessage());
		}
	}

	private Category<String, Field> loadCategory(JsonObject dictionary, String name) throws Exception {
		Category<String, Field> c = new Category<>(defaultField);
			if (dictionary.getObject(name) == null) {
				return null; // throws exeption instead to block loadind ...
			}
			c.setTypes(dictionary.getObject(name).getArray("types").toArray());
			for (Object o : dictionary.getObject(name).getArray("attributs")) {
				AAFField f = new AAFField(c, (JsonObject)o);
				c.put(f.getId(), f);
			}
		return c;
	}

	@Override
	public boolean validateField(String id, List<String> values) {
		Field f = users.get(id);
		return !validators.get(f.validator).test(values).contains(false);
	}

	@Override
	public boolean validateField(String id, String value) {
		Field f = users.get(id);
		return validators.get(f.validator).test(value);
	}

	@Override
	public Map<String, Boolean> validateFieldsList(Map<String, List<String>> fields) {
		Map<String, Boolean> result = new HashMap<>();
		for (Map.Entry<String, List<String>> entry : fields.entrySet()) {
			result.put(entry.getKey(), validateField(entry.getKey(), entry.getValue()));
		}
		return result;
	}

	@Override
	public Map<String, Boolean> validateFields(Map<String, String> fields) {
		Map<String, Boolean> result = new HashMap<>();
		for (Map.Entry<String, String> entry : fields.entrySet()) {
			result.put(entry.getKey(), validateField(entry.getKey(), entry.getValue()));
		}
		return result;
	}

	@Override
	public Field getField(String id) {
		return users.get(id);
	}

	@Override
	public void link(String oneField, String specificField) {
		throw new UnsupportedOperationException("Not supported yet.");
	}
}
