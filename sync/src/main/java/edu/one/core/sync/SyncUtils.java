/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.one.core.sync;

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
}
