/*
 * Copyright © "Open Digital Education", 2018
 *
 * This program is published by "Open Digital Education".
 * You must indicate the name of the software and the company in any production /contribution
 * using the software and indicate on the home page of the software industry in question,
 * "powered by Open Digital Education" with a reference to the website: https://opendigitaleducation.com/.
 *
 * This program is free software, licensed under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation, version 3 of the License.
 *
 * You can redistribute this application and/or modify it since you respect the terms of the GNU Affero General Public License.
 * If you modify the source code and then use this modified source code in your creation, you must make available the source code of your modifications.
 *
 * You should have received a copy of the GNU Affero General Public License along with the software.
 * If not, please see : <http://www.gnu.org/licenses/>. Full compliance requires reading the terms of this license and following its directives.
 */

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
