package edu.one.core.datadictionary.dictionary;

import java.util.List;
import java.util.Map;

public interface Dictionary {

	boolean validateField(String name, List<String> values);

	boolean validateField(String name, String value);

	Map<String, Boolean> validateFieldsList(Map<String,List<String>> fields);

	Map<String, Boolean> validateFields(Map<String,String> fields);

	void link(String oneField, String specificField);

	Field getField(String name);
}
