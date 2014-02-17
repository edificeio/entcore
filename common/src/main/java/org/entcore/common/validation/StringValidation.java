package org.entcore.common.validation;

import java.util.regex.Pattern;

public class StringValidation {

	private static final Pattern emailPattern = Pattern.compile("^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,4}$");
	private static final Pattern uuidPattern = Pattern.compile(
			"^[0-9a-f]{8}-[0-9a-f]{4}-[1-5][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$");

	public static boolean isEmail(String email) {
		return email != null && emailPattern.matcher(email).matches();
	}

	public static boolean isUUID(String uuid) {
		return uuid != null && uuidPattern.matcher(uuid).matches();
	}

}
