package edu.one.core.datadictionary.validation;

import java.util.List;

public interface Validator {

	boolean test(String s);

	List<Boolean> test(List<String> l);
}