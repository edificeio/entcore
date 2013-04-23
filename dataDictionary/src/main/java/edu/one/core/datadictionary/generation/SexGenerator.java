package edu.one.core.datadictionary.generation;

import java.util.Arrays;

public class SexGenerator extends FieldGenerator {

	@Override
	public String generate(String... in) {
		String civility = in[0];
		return Arrays.asList("M","M.").contains(civility) ? "H" : "F";
	}
}
