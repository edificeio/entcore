package edu.one.core.infra.http;

import org.vertx.java.core.http.HttpServerRequest;
import org.vertx.java.core.http.HttpServerResponse;

public class ETag {

	public static void addHeader(HttpServerResponse response, String fileId) {
		response.headers().add("ETag", fileId);
	}

	public static boolean check(HttpServerRequest request, String fileId) {
		String inm = request.headers().get("If-None-Match");
		if (inm != null) {
			return inm.equals(fileId);
		}
		return false;
	}

}
