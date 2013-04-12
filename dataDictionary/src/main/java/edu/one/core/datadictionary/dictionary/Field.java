package edu.one.core.datadictionary.dictionary;

public class Field {

	protected String name;
	protected String label;
	protected String note;
	protected boolean isRequired;
	protected boolean isMultiple;

	// tmp public
	public String validator;

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getLabel() {
		return label;
	}

	public void setLabel(String label) {
		this.label = label;
	}

	public String getNote() {
		return note;
	}

	public void setNote(String note) {
		this.note = note;
	}

	public boolean isIsRequired() {
		return isRequired;
	}

	public void setIsRequired(boolean isRequired) {
		this.isRequired = isRequired;
	}

	public boolean isIsMultiple() {
		return isMultiple;
	}

	public void setIsMultiple(boolean isMultiple) {
		this.isMultiple = isMultiple;
	}

	public void setValidator(String validator) {
		this.validator = validator;
	}

	public String getValidator() {
		return validator;
	}

}