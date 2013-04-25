package edu.one.core.datadictionary.dictionary;

import edu.one.core.datadictionary.generation.Generator;
import java.util.ArrayList;
import java.util.List;

public class Field {

	protected String id;
	protected String label;
	protected String note;
	protected boolean isRequired;
	protected boolean isMultiple;
	protected boolean isEditable;
	protected String validator;
	protected List<String> restrictions;
	protected Generator generator;

	protected Category parent;

	public Field() {
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
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
	public boolean isIsEditable() {
		return isEditable;
	}

	public void setIsEditable(boolean isEditable) {
		this.isEditable = isEditable;
	}

	public List<String> getRestrictions() {
		return restrictions;
	}

	public Generator getGenerator() {
		return generator;
	}

	public void setGenerator(Generator generator) {
		this.generator = generator;
	}

	public void setRestrictions(Object[] restrictions) {
		this.restrictions = new ArrayList<>();
		for (Object o : restrictions) {
			this.restrictions.add((String)o);
		}
	}

}