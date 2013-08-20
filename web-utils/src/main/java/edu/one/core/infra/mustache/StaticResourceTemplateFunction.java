package edu.one.core.infra.mustache;

public class StaticResourceTemplateFunction extends VertxTemplateFunction {

	private String host;
	private String infraPort;

	// TODO : make configurable
	private final static String publicDir = "public";
	private final static String protocol = "http://";

	public StaticResourceTemplateFunction() {}

	public StaticResourceTemplateFunction(String infraPort) {
		this.infraPort = infraPort;
	}

	@Override
	public String apply(String path) {
		host = infraPort == null ?
				request.headers().get("Host") :
				request.headers().get("Host").split(":")[0] + ":" + infraPort;
		return protocol
				+ host
				+ "/" + publicDir 
				+ "/" + path;
	}
}
