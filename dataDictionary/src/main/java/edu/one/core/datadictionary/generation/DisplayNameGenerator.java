package edu.one.core.datadictionary.generation;

public class DisplayNameGenerator extends FieldGenerator {

	@Override
	public String generate(String... in) {
		String firstName = in[0];
		String lastName = in[1];
		return firstName + " " + lastName;
	}

}
