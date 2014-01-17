package org.entcore.datadictionary.generation;

public class ChargeEnseignementGenerator extends FieldGenerator {

	@Override
	public String generate(String... in) {
		if (in.length > 0) {
			if ("ENSEIGNANT".equals(in[0])) {
				return "O";
			} else {
				return "N";
			}
		} else {
			return "";
		}
	}
}
