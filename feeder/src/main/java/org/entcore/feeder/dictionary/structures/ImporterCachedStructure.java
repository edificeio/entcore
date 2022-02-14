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

package org.entcore.feeder.dictionary.structures;

import java.util.Collections;
import java.util.Set;
import java.util.HashSet;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import static fr.wseduc.webutils.Utils.isNotEmpty;

public class ImporterCachedStructure extends ImporterStructure
{
	protected final Set<String> classes = Collections.synchronizedSet(new HashSet<String>());
	protected final Set<String> groups = Collections.synchronizedSet(new HashSet<String>());
	protected final Set<String> classesGroups = Collections.synchronizedSet(new HashSet<String>());

	public ImporterCachedStructure(JsonObject struct)
	{
		super(struct);
	}

	protected ImporterCachedStructure(String externalId, JsonObject struct) {
		super(externalId, struct);
	}

	protected ImporterCachedStructure(JsonObject struct, JsonArray groups, JsonArray classes) {
		super(struct);
		if (groups != null) {
			for (Object o : groups) {
				if (!(o instanceof String)) continue;
				this.groups.add((String) o);
			}
		}
		if (classes != null) {
			for (Object o : classes) {
				if (!(o instanceof String)) continue;
				this.classes.add((String) o);
			}
		}
	}

    @Override
    public void createClassIfAbsent(String classExternalId, String name) {
		if (classes.add(classExternalId)) {
            super.createClassIfAbsent(classExternalId, name);
		}
	}

    @Override
	public void updateClassName(String classExternalId, String name) {
		if (classes.contains(classExternalId)) {
            super.updateClassName(classExternalId, name);
		}
	}

    @Override
	public void createFunctionalGroupIfAbsent(String groupExternalId, String name, String source) {
		if (groups.add(groupExternalId)) {
            super.createFunctionalGroupIfAbsent(groupExternalId, name, source);
		}
	}

    @Override
	public void createFunctionGroupIfAbsent(String groupExternalId, String name, String label, String source) {
		if (isNotEmpty(label) && groups.add(groupExternalId)) {
            super.createFunctionGroupIfAbsent(groupExternalId, name, label, source);
		}
	}

	@Override
	public String createHeadTeacherGroupIfAbsent()
	{
		String structureGroupExternalId = this.getHeadTeacherGroupExternalId();
		if (groups.add(structureGroupExternalId)) {
			return super.createHeadTeacherGroupIfAbsent();
		}
		return structureGroupExternalId;
	}

	@Override
	public String[] createHeadTeacherGroupIfAbsent(String classExternalId, String name)
	{
		String structureGroupExternalId = this.getHeadTeacherGroupExternalId();
		String classGroupExternalId = this.getClassHeadTeacherGroupExternalId(classExternalId);
		if (classesGroups.add(classGroupExternalId)) {
			return super.createHeadTeacherGroupIfAbsent(classExternalId, name);
		}
		return new String[]{structureGroupExternalId, classGroupExternalId};
	}

    @Override
	public String createDirectionGroupIfAbsent() {
		String dirGroupId = this.getDirectionGroupExternalId();
		if (groups.add(dirGroupId)) {
            super.createDirectionGroupIfAbsent();
        }
		return dirGroupId;
	}
}
