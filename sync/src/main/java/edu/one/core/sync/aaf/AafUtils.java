/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.one.core.sync.aaf;

import edu.one.core.sync.aaf.AafConstantes;

/**
 *
 * @author bperez
 */
public class AafUtils {
	public static String normalizeRef(String name) {
		return name
				.replaceAll(AafConstantes.AAF_SEPARATOR, "_")
				.replaceAll(" ", "")
				.replaceAll("-", "_")
				.replaceAll("\\(", "_")
				.replaceAll("\\)", "");
	}
}
