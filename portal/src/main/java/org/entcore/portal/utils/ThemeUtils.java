/* Copyright © WebServices pour l'Éducation, 2014
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
 *
 */

package org.entcore.portal.utils;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;

import java.io.File;
import java.util.*;

public class ThemeUtils {


	public static void availableThemes(final Vertx vertx, String themeDirectory, final boolean fullPath,
			final Handler<List<String>> themes) {
		vertx.fileSystem().readDir(themeDirectory, new Handler<AsyncResult<List<String>>>() {
			@Override
			public void handle(AsyncResult<List<String>> event) {
				if (event.succeeded() && event.result().size() > 0) {
					if (fullPath) {
						themes.handle(event.result());
					} else {
						final List<String> files = event.result();
						final int idx = files.get(0).lastIndexOf(File.separatorChar);
						List<String> t = new ArrayList<>();
						for (int i = 0; i < files.size(); i++) {
							String file = files.get(i);
							if(vertx.fileSystem().propsBlocking(file).isDirectory()) {
								if (idx > -1 && file.length() > idx + 1) {
									file = file.substring(idx + 1);
								}
								t.add(file);
							}
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
