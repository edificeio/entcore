package edu.one.core.datadictionary.dictionary;

import java.util.Map;

public interface Dictionary {

	boolean validateField(String name, String value);

	Map<String, Boolean> validateFields(Map<String,String> fileds);

	void link(String oneField, String specificField);

	Field getField(String name);
}
