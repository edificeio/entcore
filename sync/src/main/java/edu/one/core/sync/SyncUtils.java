/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.one.core.sync;

import edu.one.core.sync.aaf.AafConstantes;
import java.text.Normalizer;
import java.util.Arrays;
import java.util.List;

/**
 *
 * @author bperez
 */
public class SyncUtils {
	public static List<String> strToList(String chaine) {
		String[] array = {chaine};
		return Arrays.asList(array);
	}

	public static String removeAccents(String str) {
		return Normalizer.normalize(str, Normalizer.Form.NFD)
				.replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
	}

	public static String generateLogin(String name, String lastName) {
		String login = removeAccents(name).replaceAll(" ", "-").toLowerCase()
				+ "." + removeAccents(lastName).replaceAll(" ", "-").toLowerCase();
		return login.replaceAll("'", "");
	}

	public static String generateDisplayName(String name, String lastName) {
		return name + " " + lastName;
	}

	public static String generateSex(String civility) {
		if ("M".equals(civility) || "M.".equals(civility)) {
			return "H";
		} else {
			return "F";
		}
	}

	public static String generatePassword() {
		String password = "";
		for(int i = 0; i < 6; i++) {
			password += AafConstantes.alphabet[
					Integer.parseInt(Long.toString(Math.abs(Math.round(Math.random() * 27D))))];
        }
		return password;
    }
}
