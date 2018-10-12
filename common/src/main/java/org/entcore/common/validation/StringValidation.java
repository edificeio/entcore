/* Copyright © "Open Digital Education", 2014
 *
 * This program is published by "Open Digital Education".
 * You must indicate the name of the software and the company in any production /contribution
 * using the software and indicate on the home page of the software industry in question,
 * "powered by Open Digital Education" with a reference to the website: https://opendigitaleducation.com/.
 *
 * This program is free software, licensed under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation, version 3 of the License.
 *
 * You can redistribute this application and/or modify it since you respect the terms of the GNU Affero General Public License.
 * If you modify the source code and then use this modified source code in your creation, you must make available the source code of your modifications.
 *
 * You should have received a copy of the GNU Affero General Public License along with the software.
 * If not, please see : <http://www.gnu.org/licenses/>. Full compliance requires reading the terms of this license and following its directives.

 *
 */

package org.entcore.common.validation;

import java.text.Normalizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class StringValidation {

	private static final String[] alphabet =
		{"a","b","c","d","e","f","g","h","j","k","m","n","p","r","s","t","v","w","x","y","z","3","4","5","6","7","8","9"};

	private static final Pattern emailPattern = Pattern.compile("^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,4}$");
	private static final Pattern phonePattern = Pattern.compile("^(00|\\+)?(?:[0-9] ?-?\\.?){6,15}$");
	private static final Pattern uuidPattern = Pattern.compile(
			"^[0-9a-f]{8}-[0-9a-f]{4}-[1-5][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$");
	private static final Pattern absoluteDocumentUriPattern = Pattern.compile(
			"^/workspace/document/[0-9a-f]{8}-[0-9a-f]{4}-[1-5][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$");
	private static final Pattern uaiPattern = Pattern.compile("^[0-9]{7}[A-Z]$");
	private static final Pattern frenchDatePatter = Pattern.compile("^([0-9]{2})/([0-9]{2})/([0-9]{4})$");


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

	public static String removeAccents(String str) {
		return Normalizer.normalize(str, Normalizer.Form.NFD)
				.replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
	}

	public static String generateRandomCode(int size){
		StringBuilder builder = new StringBuilder();
		for(int i = 0; i < size; i++){
			builder.append(alphabet[Integer.parseInt(Long.toString(Math.abs(Math.round(Math.random() * 27D))))]);
		}
		return builder.toString();
	}

	public static String obfuscateMail(String mail){
		if(mail.split("@").length < 2)
			return mail;

		final String firstPart = mail.split("@")[0];
		return (firstPart.length() > 4 ?
			firstPart.substring(0, 1) +
			firstPart.substring(1, firstPart.length() - 1).replaceAll(".", ".") +
			firstPart.substring(firstPart.length() - 1, firstPart.length()) :
			firstPart.replaceAll(".", ".")) + "@" + mail.split("@")[1] ;
	}
	public static String obfuscateMobile(String mobile){
		return mobile.length() > 4 ?
			mobile.substring(0, mobile.length() - 4).replaceAll(".", ".") +
			mobile.substring(mobile.length() - 4) : mobile.replaceAll(".", ".");
	}

	public static String formatPhone(String phone){
		final String formattedPhone = phone.replaceAll("[^0-9\\\\+]", "");
		return !formattedPhone.startsWith("00") && !formattedPhone.startsWith("+") && formattedPhone.startsWith("0") && formattedPhone.length() == 10 ?
				formattedPhone.replaceFirst("0", "+33") :
				formattedPhone;
	}

	public static String convertDate(String date) {
		Matcher m;
		if (date != null && (m = frenchDatePatter.matcher(date)).find()) {
			return m.group(3) + "-" + m.group(2) + "-" + m.group(1);
		}
		return date;
	}

	public static String cleanId(String id) {
		return cleanId(id, "emptyId");
	}

	public static String cleanId(String id, String defaultValue) {
		if (id != null) {
			return id.replaceAll("\\W+", "-");
		}
		return defaultValue;
	}

	public static String sanitize(String field) {
		return removeAccents(field)
				.replaceAll("\\s+", "")
				.replaceAll("\\-","")
				.replaceAll("'","")
				.toLowerCase();
	}

}
