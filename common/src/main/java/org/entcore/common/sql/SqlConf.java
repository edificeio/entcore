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

package org.entcore.common.sql;

public class SqlConf {

	private String table;
	private String schema = "";
	private String resourceIdLabel = "id";
	private String shareTable = "shares";

	public String getTable() {
		return table;
	}

	public void setTable(String table) {
		this.table = table;
	}

	public String getSchema() {
		return schema;
	}

	public void setSchema(String schema) {
		this.schema = (schema != null && !schema.trim().isEmpty()) ? schema + "." : "";
	}

	public String getResourceIdLabel() {
		return resourceIdLabel;
	}

	public void setResourceIdLabel(String resourceIdLabel) {
		this.resourceIdLabel = resourceIdLabel;
	}

	public String getShareTable() {
		return shareTable;
	}

	public void setShareTable(String shareTable) {
		this.shareTable = shareTable;
	}

}
