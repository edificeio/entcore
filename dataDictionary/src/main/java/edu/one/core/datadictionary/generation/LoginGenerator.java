package edu.one.core.datadictionary.generation;

import java.text.Normalizer;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;


public class LoginGenerator extends FieldGenerator {

	private static final Set<String> s = Collections.synchronizedSet(new HashSet<String>());

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
			int i = 2;
			String l = login + "";
			while (!s.add(l)) {
				l = login + i++;
			}
			return l.replaceAll("'", "");
		} else {
			return "";
		}
	}

}
