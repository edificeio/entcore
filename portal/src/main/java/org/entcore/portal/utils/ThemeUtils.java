/*
 * Copyright. Tous droits réservés. WebServices pour l’Education.
 */

package org.entcore.portal.utils;

import org.vertx.java.core.AsyncResult;
import org.vertx.java.core.Handler;
import org.vertx.java.core.Vertx;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class ThemeUtils {


	public static void availableThemes(Vertx vertx, String themeDirectory, final boolean fullPath,
			final Handler<List<String>> themes) {
		vertx.fileSystem().readDir(themeDirectory, new Handler<AsyncResult<String[]>>() {
			@Override
			public void handle(AsyncResult<String[]> event) {
				if (event.succeeded() && event.result().length > 0) {
					if (fullPath) {
						themes.handle(Arrays.asList(event.result()));
					} else {
						final String[] files = event.result();
						final int idx = files[0].lastIndexOf(File.separatorChar);
						List<String> t = new ArrayList<>();
						for (int i = 0; i < files.length; i++) {
							String file = files[i];
							if (idx > -1 && file.length() > idx + 1) {
								file = file.substring(idx + 1);
							}
							if ("css".equals(file) || "fonts".equals(file) || "img".equals(file)) continue;
							t.add(file);
						}
						themes.handle(t);
					}
				} else {
					themes.handle(Collections.<String>emptyList());
				}
			}
		});
	}

}
