package edu.one.core.portal.mustache;

import edu.one.core.infra.mustache.VertxTemplateFunction;
import java.io.File;
import java.nio.file.FileSystem;

public class AssetResourceTemplateFunction extends VertxTemplateFunction {
	private String host;
	private final static String protocol = "http://";
	private final static String assetDir = "assets/templates";
	private  String themeName;

	public AssetResourceTemplateFunction(String themeName) {
		this.themeName = themeName;
	}

	@Override
	public String apply(String path) {
		return protocol
				+ request.headers().get("Host")
				+ "/" + assetDir
				+ "/" + themeName
				+ "/" + path;
	}

}