package org.entcore.datadictionary.generation;

public interface Generator {

	void setInputFileds(String... fields);
	String[] getInputFields();
	String generate(String... values);
}
