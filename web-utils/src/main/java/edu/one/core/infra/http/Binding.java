package edu.one.core.infra.http;

import java.util.regex.Pattern;

public class Binding {

	private final HttpMethod method;
	private final Pattern uriPattern;
	private final String serviceMethod;

	public Binding(HttpMethod method, Pattern uriPattern, String serviceMethod) {
		this.method = method;
		this.uriPattern = uriPattern;
		this.serviceMethod = serviceMethod;
	}

	public HttpMethod getMethod() {
		return method;
	}

	public Pattern getUriPattern() {
		return uriPattern;
	}

	public String getServiceMethod() {
		return serviceMethod;
	}

}
