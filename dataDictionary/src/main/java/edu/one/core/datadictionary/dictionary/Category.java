package edu.one.core.datadictionary.dictionary;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class Category<String, F extends Field> extends HashMap<String, F> {

	private F defaultValue;
	private List<String> types;

	public Category(F defaultValue) {
		this.defaultValue = defaultValue;
	}

	@Override
	public F get(Object key) {
		if (!super.containsKey(key)) {
			return defaultValue;
		}
		return super.get(key);
	}

	public List<String> getTypes() {
		return types;
	}

	public void setTypes(Object[] types) {
		this.types = new ArrayList<>();
		for (Object o : types) {
			this.types.add((String)o);
		}
	}
}
