package edu.one.core.infra;

public class Utils {

	public static <T> T getOrElse(T value, T defaultValue) {
		return getOrElse(value, defaultValue, true);
	}

	public static <T> T getOrElse(T value, T defaultValue, boolean allowEmpty) {
		if (value != null && (allowEmpty || !value.toString().trim().isEmpty())) {
			return value;
		}
		return defaultValue;
	}

}
