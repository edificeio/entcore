package org.entcore.common.validation;

import java.util.regex.Pattern;

public class StringValidation {

	private static final Pattern emailPattern = Pattern.compile("^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,4}$");

	public static boolean isEmail(String email) {
		return email != null && emailPattern.matcher(email).matches();
	}

}
