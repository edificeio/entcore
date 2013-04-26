package edu.one.core.datadictionary.generation;

public class ChargeEnseignementGenerator extends FieldGenerator {

	@Override
	public String generate(String... in) {
		if ("ENSEIGNANT".equals(in[0])) {
			return "O";
		} else {
			return "N";
		}
	}
}
