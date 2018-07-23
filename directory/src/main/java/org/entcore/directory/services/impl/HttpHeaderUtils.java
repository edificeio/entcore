package org.entcore.directory.services.impl;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import io.vertx.core.MultiMap;

public class HttpHeaderUtils {
	static DateFormat httpDateFormat = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz", Locale.ENGLISH);
	static {
		httpDateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
	}

	public static void addHeaderLastModified(MultiMap headers, Date modifiedDate) {
		headers.add("Last-Modified", httpDateFormat.format(modifiedDate));
		headers.add("Cache-Control", "must-revalidate, max-age=0");
	}

	public static boolean checkIfModifiedSince(MultiMap headers, Date refDate) {
		String ifModified = headers.get("If-Modified-Since");
		if (ifModified != null) {
			try {
				Date lastTime = httpDateFormat.parse(ifModified);
				Instant refDateInstant = refDate.toInstant().truncatedTo(ChronoUnit.SECONDS);
				Instant lastTimeInstant = lastTime.toInstant().truncatedTo(ChronoUnit.SECONDS);
				return refDateInstant.isAfter(lastTimeInstant);
			} catch (ParseException e) {
				return true;
			}
		}
		return true;
	}
}
