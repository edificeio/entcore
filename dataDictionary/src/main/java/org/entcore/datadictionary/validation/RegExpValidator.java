package org.entcore.datadictionary.validation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/*
 * Regexp Validator Factory
 */
public class RegExpValidator {

	public static Map<String, String> types = new HashMap<>();
	static {
		types.put("email", "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,4}$");
		types.put("zipCode", "^[0-9]{5}$");
		types.put("phone", "^0[1-9][0-9]{8}$");
		types.put("mobilePhone", "^0[67][0-9]{8}$");
		types.put("firstName", "^\\D{0,38}$");
		types.put("lastName", "^\\D{0,38}$");
		types.put("hour", "^([01][0-9]|2[0-3])$"); // 00-23¶
		types.put("minute", "^[0-5][0-9]$"); // 00-59
		types.put("birthDate","(0[1-9]|[12][0-9]|3[01])/(0[1-9]|1[012])/((19|20)\\d\\d)");
	}

	public static Validator instance(String regexpKey, String regexpValue) throws Exception {
		types.put(regexpKey, regexpValue);
		return instance(regexpKey);
	}

	public static Validator instance(final String regexpKey) throws Exception {
		return new Validator() {
			private Pattern p;
			{
				if (types.get(regexpKey) == null) {
					throw new Exception("datadictionnary.error.regexUndefined");
				}
				p = Pattern.compile(types.get(regexpKey));
			}

			@Override
			public boolean test(String s) {
				Matcher m = p.matcher(s);
				return m.matches();
			}

			@Override
			public List<Boolean> test(List<String> l) {
				List<Boolean> result = new ArrayList<>();
				for (String s : l) {
					result.add(test(s));
				}
				return result;
			}
		};
	}

	public static Map<String, Validator> all() throws Exception {
		Map<String,Validator> validators = new HashMap<>();
		for (String regexpKey : types.keySet()) {
			validators.put(regexpKey, RegExpValidator.instance(regexpKey));
		}
		return validators;
	}

}