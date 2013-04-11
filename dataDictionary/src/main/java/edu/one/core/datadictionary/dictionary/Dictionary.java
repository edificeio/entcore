package edu.one.core.datadictionary.dictionary;

import java.util.List;
import java.util.Map;

public interface Dictionary {

	boolean validateField(String name, List<String> values);

	Map<String, Boolean> validateFields(Map<String,List<String>> fields);

	void link(String oneField, String specificField);

	Field getField(String name);
}
