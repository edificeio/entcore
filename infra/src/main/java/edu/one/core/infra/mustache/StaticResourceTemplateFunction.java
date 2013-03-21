package edu.one.core.infra.mustache;

import com.github.mustachejava.TemplateFunction;
import org.vertx.java.core.http.HttpServerRequest;

public class StaticResourceTemplateFunction implements TemplateFunction{

	private HttpServerRequest request;

	// TODO : make configurable
	private final static String publicDir = "public";
	private final static String protocol = "http://";

	public StaticResourceTemplateFunction(HttpServerRequest request) {
		this.request = request;
	}

	@Override
	public String apply(String path) {
		return protocol
				+ request.headers().get("Host")
				+ "/" + publicDir 
				+ "/" + path;
	}
}
