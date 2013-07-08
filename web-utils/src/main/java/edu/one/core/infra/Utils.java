package edu.one.core.infra;

public class Utils {

	public static <T> T getOrElse(T value, T defaultValue) {
		if (value != null) {
			return value;
		}
		return defaultValue;
	}

}
