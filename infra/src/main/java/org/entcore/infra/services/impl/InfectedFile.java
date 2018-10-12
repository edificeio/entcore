/*
 * Copyright © "Open Digital Education", 2017
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

package org.entcore.infra.services.impl;

import java.io.File;

public class InfectedFile {

	private final String path;
	private final String virus;
	private String id;
	private long timerId;
	private String name;
	private String owner;
	private String application;

	public InfectedFile(String path, String virus) {
		this.path = path;
		this.virus = virus;
	}

	public String getPath() {
		return path;
	}

	public String getVirus() {
		return virus;
	}

	public String getId() {
		if (id == null) {
			final String [] items = path.split(File.separator);
			id = items[items.length -1];
		}
		return id;
	}

	public long getTimerId() {
		return timerId;
	}

	public void setTimerId(long timerId) {
		this.timerId = timerId;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getOwner() {
		return owner;
	}

	public void setOwner(String owner) {
		this.owner = owner;
	}

	public String getApplication() {
		return application != null ? application.toLowerCase() : null;
	}

	public void setApplication(String application) {
		this.application = application;
	}

}
