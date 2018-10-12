/*
 * Copyright © "Open Digital Education", 2016
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

package org.entcore.feeder.utils;

import com.opencsv.CSVReader;
import com.opencsv.CSVWriter;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.file.AsyncFile;
import io.vertx.core.file.OpenOptions;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

import java.io.*;
import java.security.NoSuchAlgorithmException;

import static fr.wseduc.webutils.Utils.isNotEmpty;

public class CSVUtil {

	public static final String UTF8_BOM = "\uFEFF";
	private static final Logger log = LoggerFactory.getLogger(CSVUtil.class);

	private CSVUtil() {}

	public static JsonObject getStructure(String p) throws UnsupportedEncodingException, NoSuchAlgorithmException {
		String dirName = p.substring(p.lastIndexOf(File.separatorChar) + 1);
		String [] n = dirName.split("_");
		JsonObject structure = new JsonObject();
		int idx = n[0].indexOf("@");
		if (idx >= 0) {
			structure.put("name", n[0].substring(0, idx));
			structure.put("externalId", n[0].substring(idx + 1));
		} else {
			structure.put("name", n[0]);
			structure.put("externalId", Hash.sha1(dirName.getBytes("UTF-8")));
		}
		if (n.length > 1 && isNotEmpty(n[1])) {
			structure.put("UAI", n[1]);
		}
		if (n.length > 2 && isNotEmpty(n[2])) {
			structure.put("overrideClass", n[2]);
		}
		return structure;
	}

	public static void getCharset(Vertx vertx, String path, final Handler<String> handler) {
		vertx.fileSystem().open(path, new OpenOptions(), new Handler<AsyncResult<AsyncFile>>() {
			@Override
			public void handle(final AsyncResult<AsyncFile> event) {
				if (event.succeeded()) {
					event.result().read(Buffer.buffer(4), 0, 0, 4, new Handler<AsyncResult<Buffer>>() {
						@Override
						public void handle(AsyncResult<Buffer> ar) {
							event.result().close();
							if (ar.succeeded() && ar.result().toString("UTF-8").startsWith(UTF8_BOM)) {
								handler.handle("UTF-8");
							} else {
								handler.handle("ISO-8859-1");
							}
						}
					});
				} else {
					handler.handle("ISO-8859-1");
				}
			}
		});
	}

	public static String getCharsetSync(String path) {
		FileInputStream fis = null;
		try {
			fis = new FileInputStream(path);
			BufferedReader r = new BufferedReader(new InputStreamReader(fis, "UTF8"));
			return r.readLine().startsWith(UTF8_BOM) ? "UTF-8" : "ISO-8859-1";
		} catch (Exception e) {
			log.error("Error when detect charset", e);
		} finally {
			if (fis != null) {
				try {
					fis.close();
				} catch (IOException e) {
					log.error("Error when close file.", e);
				}
			}
		}
		return "ISO-8859-1";
	}

	public static CSVReader getCsvReader(String file, String charset)
			throws FileNotFoundException, UnsupportedEncodingException {
		return getCsvReader(file, charset, 0);
	}

	public static CSVReader getCsvReader(String file, String charset, int skipLines)
			throws FileNotFoundException, UnsupportedEncodingException {
		return new CSVReader(new InputStreamReader(new FileInputStream(file), charset), ';', '"', skipLines);
	}

	public static CSVWriter getCsvWriter(String file, String charset) throws IOException {
		FileOutputStream fos = new FileOutputStream(file);
		if ("UTF-8".equals(charset)) {
			fos.write(UTF8_BOM.getBytes());
		}
		return new CSVWriter(new OutputStreamWriter(fos, charset), ';');
	}

	public static boolean emptyLine(String [] line) {
		if (line != null) {
			for (String s : line) {
				if (isNotEmpty(s)) {
					return false;
				}
			}
		}
		return true;
	}

}
