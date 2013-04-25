package edu.one.core.datadictionary.dictionary;

import java.util.ArrayList;
import java.util.List;

public class Category {

	private String name;
	private List<String> types;

	public Category(String name, Object[] types) {
		this.name = name;
		setTypes(types);
	}


	public List<String> getTypes() {
		return types;
	}

	public final void setTypes(Object[] types) {
		this.types = new ArrayList<>();
		for (Object o : types) {
			this.types.add((String)o);
		}
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}
}
