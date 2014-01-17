package org.entcore.datadictionary.dictionary;

import java.util.List;
import java.util.Map;

public interface Dictionary {


	boolean validateField(String name, List<String> values);

	boolean validateField(String name, String value);

	Map<String, Boolean> validateFieldsList(Map<String,List<String>> fields);

	Map<String, Boolean> validateFields(Map<String,String> fields);

	Map<String, Boolean> validateFields(Iterable<Map.Entry<String, String>> fields);

	Map<String, List<String>> generateField(Map<String, List<String>> values);

	void link(String oneField, String specificField);

	Field getField(String name);
}
