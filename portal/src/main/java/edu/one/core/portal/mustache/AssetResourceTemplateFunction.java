package edu.one.core.portal.mustache;

import edu.one.core.infra.mustache.VertxTemplateFunction;
import java.io.File;
import java.nio.file.FileSystem;

public class AssetResourceTemplateFunction extends VertxTemplateFunction {
	private String host;
	private final String protocol;
	private final static String assetDir = "assets/themes";
	private  String themeName;

	public AssetResourceTemplateFunction(String themeName) {
		this(themeName, false);
	}

	public AssetResourceTemplateFunction(String themeName, boolean https) {
		this.themeName = themeName;
		this.protocol = https ? "https://" : "http://";
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