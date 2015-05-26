/* Copyright © WebServices pour l'Éducation, 2014
 *
 * This file is part of ENT Core. ENT Core is a versatile ENT engine based on the JVM.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation (version 3 of the License).
 *
 * For the sake of explanation, any module that communicate over native
 * Web protocols, such as HTTP, with ENT Core is outside the scope of this
 * license and could be license under its own terms. This is merely considered
 * normal use of ENT Core, and does not fall under the heading of "covered work".
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *
 */

package org.entcore.common.validation;

import java.util.regex.Pattern;

public class StringValidation {

	private static final Pattern emailPattern = Pattern.compile("^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,4}$");
	private static final Pattern phonePattern = Pattern.compile("^(0|\\+33)\\s*[0-9]([-. ]?[0-9]{2}){4}$");
	private static final Pattern uuidPattern = Pattern.compile(
			"^[0-9a-f]{8}-[0-9a-f]{4}-[1-5][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$");
	private static final Pattern absoluteDocumentUriPattern = Pattern.compile(
			"^/workspace/document/[0-9a-f]{8}-[0-9a-f]{4}-[1-5][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$");
	private static final Pattern uaiPattern = Pattern.compile("^[0-9]{7}[A-Z]$");


	public static boolean isEmail(String email) {
		return email != null && emailPattern.matcher(email).matches();
	}

	public static boolean isPhone(String phone) {
		return phone != null && phonePattern.matcher(phone).matches();
	}

	public static boolean isUUID(String uuid) {
		return uuid != null && uuidPattern.matcher(uuid).matches();
	}

	public static boolean isAbsoluteDocumentUri(String uri) {
		return  uri != null && absoluteDocumentUriPattern.matcher(uri).matches();
	}

	public static boolean isUAI(String uai) {
		return uai != null && uaiPattern.matcher(uai).matches();
	}

}
