package org.entcore.datadictionary.generation;

import java.util.Arrays;

public class SexGenerator extends FieldGenerator {

	@Override
	public String generate(String... in) {
		if (in.length > 0) {
			String civility = in[0];
			return Arrays.asList("M","M.").contains(civility) ? "H" : "F";
		} else {
			return "";
		}
	}
}
