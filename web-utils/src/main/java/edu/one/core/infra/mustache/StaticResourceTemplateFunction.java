package edu.one.core.infra.mustache;

import com.github.mustachejava.TemplateFunction;
import org.vertx.java.core.http.HttpServerRequest;

public class StaticResourceTemplateFunction implements TemplateFunction{

	private String host;

	// TODO : make configurable
	private final static String publicDir = "public";
	private final static String protocol = "http://";

	public StaticResourceTemplateFunction(HttpServerRequest request) {
		host = request.headers().get("Host");
	}

	public StaticResourceTemplateFunction(HttpServerRequest request, String infraPort) {
		host = request.headers().get("Host").split(":")[0] + ":" + infraPort;
	}

	@Override
	public String apply(String path) {
		return protocol
				+ host
				+ "/" + publicDir 
				+ "/" + path;
	}
}
