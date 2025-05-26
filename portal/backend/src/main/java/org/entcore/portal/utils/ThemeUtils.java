/* Copyright © "Open Digital Education", 2014
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
