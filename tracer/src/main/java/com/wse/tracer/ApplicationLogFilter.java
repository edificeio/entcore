package com.wse.tracer;

import java.util.logging.Filter;
import java.util.logging.LogRecord;

/**
 *
 * @author dwak
 */
public class ApplicationLogFilter implements Filter {

	private String name = "";

	public ApplicationLogFilter(String appli) {
		name = appli;
	}

	@Override
	public boolean isLoggable(LogRecord record) {
		return record.getParameters()[0].equals(name);
	}

	@Override
	public String toString(){
		return this.name;
	}

}