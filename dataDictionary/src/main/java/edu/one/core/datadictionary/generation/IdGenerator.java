package edu.one.core.datadictionary.generation;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Random;

public class IdGenerator extends FieldGenerator {
	private static final char[] symbols = new char[26];
	static {
		for (int i = 0; i < 26; ++i) {
			symbols[i] = (char) ('a' + i);
		}
	}

	private final Random random = new Random();

	public String randomString(int length)	{
		char[] buf = new char[length];
		for (int i = 0; i < buf.length; ++i) {
			buf[i] = symbols[random.nextInt(symbols.length)];
		}
		return new String(buf);
	}
	
	@Override
	public String generate(String... in) {
		// TODO: injecter les codes pour construire les identifiants à partir de variables
		String codeRegion = "V";
		String codeDepartement = "0";
		
		SimpleDateFormat formater = new SimpleDateFormat("yyyyMMddHHmmssSSS");
		// identifiant au format AAF
		return (codeRegion + randomString(4) + codeDepartement + formater.format(new Date()));
	}

}
