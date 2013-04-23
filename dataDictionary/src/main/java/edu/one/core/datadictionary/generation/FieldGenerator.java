package edu.one.core.datadictionary.generation;

public abstract class FieldGenerator implements Generator {

	private String[] inputFields;

	@Override
	public void setInputFileds(String... fields) {
		inputFields = fields;
	}

	@Override
	public String[] getInputFields() {
		return inputFields;
	}

}
