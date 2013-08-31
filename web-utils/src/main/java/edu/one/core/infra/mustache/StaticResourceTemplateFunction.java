package edu.one.core.infra.mustache;

public class StaticResourceTemplateFunction extends VertxTemplateFunction {

	private final String infraPort;

	// TODO : make configurable
	private final String publicDir;
	private final static String protocol = "http://";

	public StaticResourceTemplateFunction(String publicDir) {
		this.publicDir = publicDir;
		this.infraPort = null;
	}

	public StaticResourceTemplateFunction(String publicDir, String infraPort) {
		this.publicDir = publicDir;
		this.infraPort = infraPort;
	}

	@Override
	public String apply(String path) {
		String host = request.headers().get("Host");
		if (infraPort != null && request.headers().get("X-Forwarded-For") == null) {
			host = request.headers().get("Host").split(":")[0] + ":" + infraPort;
		}
		return protocol
				+ host
				+ ((publicDir != null && publicDir.startsWith("/")) ? publicDir : "/" + publicDir)
				+ "/" + path;
	}
}
