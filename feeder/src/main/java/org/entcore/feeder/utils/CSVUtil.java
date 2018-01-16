/*
 * Copyright © WebServices pour l'Éducation, 2016
 *
 * This file is part of ENT Core. ENT Core is a versatile ENT engine based on the JVM.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation (version 3 of the License).
 *
 * For the sake of explanation, any module that communicate over native
 * Web protocols, such as HTTP, with ENT Core is outside the scope of this
 * license and could be license under its own terms. This is merely considered
 * normal use of ENT Core, and does not fall under the heading of "covered work".
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 */

package org.entcore.feeder.utils;

import com.opencsv.CSVReader;
import com.opencsv.CSVWriter;
import org.vertx.java.core.AsyncResult;
import org.vertx.java.core.Handler;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.buffer.Buffer;
import org.vertx.java.core.file.AsyncFile;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.logging.Logger;
import org.vertx.java.core.logging.impl.LoggerFactory;

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
			structure.putString("name", n[0].substring(0, idx));
			structure.putString("externalId", n[0].substring(idx + 1));
		} else {
			structure.putString("name", n[0]);
			structure.putString("externalId", Hash.sha1(dirName.getBytes("UTF-8")));
		}
		if (n.length > 1 && isNotEmpty(n[1])) {
			structure.putString("UAI", n[1]);
		}
		if (n.length > 2 && isNotEmpty(n[2])) {
			structure.putString("overrideClass", n[2]);
		}
		return structure;
	}

	public static void getCharset(Vertx vertx, String path, final Handler<String> handler) {
		vertx.fileSystem().open(path, new Handler<AsyncResult<AsyncFile>>() {
			@Override
			public void handle(final AsyncResult<AsyncFile> event) {
				if (event.succeeded()) {
					event.result().read(new Buffer(4), 0, 0, 4, new Handler<AsyncResult<Buffer>>() {
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
