package edu.one.core.datadictionary.dictionary.aaf;

import edu.one.core.datadictionary.generation.Generator;

public class AAFGenerators {

	public static Generator instance(String name, String... fields) {
		Generator g = null;
		try {
			g = Class.forName("edu.one.core.datadictionary.generation." + capitalize(name) + "Generator")
					.asSubclass(Generator.class)
					.newInstance();
			g.setInputFileds(fields);
		} catch (ClassNotFoundException | InstantiationException | IllegalAccessException ex) {
			ex.printStackTrace();
		}
		return g;
	}

	// TODO : move to String Utils
	private static String capitalize(String s) {
		return Character.toUpperCase(s.charAt(0)) + s.substring(1);
	}
}
