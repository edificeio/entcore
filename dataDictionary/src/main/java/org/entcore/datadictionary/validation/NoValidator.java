package org.entcore.datadictionary.validation;

import java.util.ArrayList;
import java.util.List;

/**
 * NoValidator don't validate anything. It just return evrytime TRUE or FALSE
 * depending on its construction.
 * @author rafik
 */
public class NoValidator implements Validator {

	private boolean result;

	public NoValidator(boolean result) {
		this.result = result;
	}

	@Override
	public boolean test(String s) {
		return result;
	}

	@Override
	public List<Boolean> test(List<String> l) {
		List<Boolean> results = new ArrayList<>();
		for (String string : l) {
			results.add(Boolean.valueOf(result));
		}
		return results;
	}
}
