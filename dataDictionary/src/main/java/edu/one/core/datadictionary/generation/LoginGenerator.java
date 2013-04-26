package edu.one.core.datadictionary.generation;

import java.text.Normalizer;


public class LoginGenerator extends FieldGenerator {

	private static String removeAccents(String str) {
		return Normalizer.normalize(str, Normalizer.Form.NFD)
				.replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
	}

	@Override
	public String generate(String... in) {
		if (in.length > 0) {
			String firstName = in[0];
			String lastName = in[1];
			String login = removeAccents(firstName).replaceAll(" ", "-").toLowerCase()
					+ "." + removeAccents(lastName).replaceAll(" ", "-").toLowerCase();
			//TODO: vérifier l'unicité du login
			return login.replaceAll("'", "");
		} else {
			return "";
		}
	}

}
