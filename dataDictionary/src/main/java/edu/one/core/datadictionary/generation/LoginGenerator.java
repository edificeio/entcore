package edu.one.core.datadictionary.generation;

import java.text.Normalizer;


public class LoginGenerator extends FieldGenerator {

	private static String removeAccents(String str) {
		return Normalizer.normalize(str, Normalizer.Form.NFD)
				.replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
	}

	@Override
	public String generate(String... in) {
		String firstName = in[0];
		String lastName = in[1];
		String login = removeAccents(firstName).replaceAll(" ", "-").toLowerCase()
				+ "." + removeAccents(lastName).replaceAll(" ", "-").toLowerCase();
		return login.replaceAll("'", "");
	}

}
