/*
 * Copyright © "Open Digital Education", 2014
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

package org.entcore.common.mongodb;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class MongoDbConf {

	private String collection;
	private String resourceIdLabel = "id";
	private Set<String> searchTextFields = new HashSet<>();
	private MongoDbConf() {}

	private static class MongoDbConfHolder {
		private static final MongoDbConf instance = new MongoDbConf();
	}

	public static MongoDbConf getInstance() {
		return MongoDbConfHolder.instance;
	}

	public String getCollection() {
		return collection;
	}

	public void setCollection(String collection) {
		this.collection = collection;
	}

	public String getResourceIdLabel() {
		return resourceIdLabel;
	}

	public void setResourceIdLabel(String resourceIdLabel) {
		this.resourceIdLabel = resourceIdLabel;
	}

	public Set<String> getSearchTextFields() {
		return this.searchTextFields;
	}

	public void addSearchTextField(String field) {
		this.searchTextFields.add(field);
	}

	public void addSearchTextFields(String... fields) {
		this.searchTextFields.addAll(Arrays.asList(fields));
	}
}
