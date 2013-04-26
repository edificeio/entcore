package edu.one.core.datadictionary.dictionary;

import edu.one.core.datadictionary.dictionary.aaf.AAFField;
import edu.one.core.datadictionary.validation.NoValidator;
import edu.one.core.datadictionary.validation.RegExpValidator;
import edu.one.core.datadictionary.validation.Validator;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.logging.Logger;
import org.vertx.java.platform.Container;

public class DefaultDictionary implements Dictionary {

	protected Logger logger;

	protected Map<String,Category> categories; // is it useful ?
	protected Map<String, Field> fields;
	protected List<Field> generatedFields;
	protected Map<String, Validator> validators;

	public DefaultDictionary(Vertx vertx, Container container, String file) {
		logger = container.logger();
		try {
			validators = RegExpValidator.all();
			validators.put(null, new NoValidator(true)); // If no validator return true
			generatedFields = new ArrayList<>();
			JsonObject d = new JsonObject(vertx.fileSystem().readFileSync(file).toString());
			fields = new HashMap<>();
			categories = new HashMap<>();

			for (String name : d.getFieldNames()) {
				categories.put(name, new Category(name,d.getObject(name).getArray("types").toArray()));
				for (Object o : d.getObject(name).getArray("attributs")) {
					AAFField f = new AAFField(categories.get(name), (JsonObject)o);
					fields.put(f.getId(), f);
					if (f.getGenerator() != null) {
						generatedFields.add(f);
					}
				}

			}
		} catch (Exception ex) {
			ex.printStackTrace();
			logger.error(ex.getMessage());
		}
	}

	@Override
	public boolean validateField(String id, List<String> values) {
		Field f = getField(id);
		return !validators.get(f.validator).test(values).contains(false);
	}

	@Override
	public boolean validateField(String id, String value) {
		Field f = getField(id);
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
	public Map<String, List<String>> generateField(Map<String, List<String>> attrs) {
		for (Field f : generatedFields) {
			List<String> values = new ArrayList<>();
			for (String inputField : f.getGenerator().getInputFields()) {
				if (attrs.containsKey(inputField)) {
					values.add(attrs.get(inputField).get(0));
				}
			}
			attrs.put(f.getId(), Arrays.asList(f.getGenerator().generate(values.toArray(new String[]{}))));
		}
		return attrs;
	}

	
	@Override
	public Field getField(String id) {
		Field f = fields.get(id);
		return f == null ? new Field() : f;
	}

	@Override
	public void link(String oneField, String specificField) {
		throw new UnsupportedOperationException("Not supported yet.");
	}
}
