package edu.one.core.datadictionary.generation;

public class ActivationCodeGenerator extends FieldGenerator {

	private final String[] alphabet =
		{"a","b","c","d","e","f","g","h","j","k","m","n","p","r","s","t","v","w","x","y","z","3","4","5","6","7","8","9"};

	@Override
	public String generate(String... in) {
		String password = "";
		for(int i = 0; i < 6; i++) {
			password += alphabet[Integer.parseInt(Long.toString(Math.abs(Math.round(Math.random() * 27D))))];
		}
		return password;
	}

}
