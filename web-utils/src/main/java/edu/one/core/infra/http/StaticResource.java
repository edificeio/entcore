package edu.one.core.infra.http;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import org.vertx.java.core.http.HttpServerRequest;
import org.vertx.java.core.http.HttpServerResponse;

public class StaticResource {

	private static final SimpleDateFormat format =
			new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz", Locale.UK);
	static {
		format.setTimeZone(TimeZone.getTimeZone("GMT"));
	}

	public static void addLastModifiedHeader(HttpServerResponse response, Date resourceLastModified) {
		response.headers().add("Last-Modified", format.format(resourceLastModified));
	}

	public static void addLastModifiedHeader(HttpServerResponse response, String resourceLastModified) {
		response.headers().add("Last-Modified", resourceLastModified);
	}

	public static boolean checkLastModified(HttpServerRequest request, String resourceLastModified) {
		String ims = request.headers().get("If-Modified-Since");
		if (ims != null && resourceLastModified != null) {
			try {
				Date imsDate = format.parse(ims);
				Date rlm = format.parse(resourceLastModified);
				return imsDate != null && rlm != null &&
						imsDate.getTime() >= rlm.getTime();
			} catch (ParseException e) {
				return false;
			}
		}
		return false;
	}

	public static void serveRessource(HttpServerRequest request, String ressourcePath,
			String resourceLastModified) {
		request.response().headers().add("Cache-Control", "max-age=0, no-cache, must-revalidate");
		addLastModifiedHeader(request.response(), resourceLastModified);
		if (checkLastModified(request, resourceLastModified)) {
			request.response().setStatusCode(304).end();
		} else {
			request.response().sendFile(ressourcePath);
		}
	}

	public static String formatDate(Date lastModified) {
		return format.format(lastModified);
	}

}
