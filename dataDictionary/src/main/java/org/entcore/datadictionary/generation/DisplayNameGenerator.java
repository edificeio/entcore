package org.entcore.datadictionary.generation;

public class DisplayNameGenerator extends FieldGenerator {

	@Override
	public String generate(String... in) {
		if (in.length > 0) {
			String firstName = in[0];
			String lastName = in[1];
			return firstName + " " + lastName;
		} else {
			return "";
		}
	}
}
