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

package org.entcore.feeder.aaf;

import org.entcore.feeder.dictionary.structures.DefaultFunctions;
import org.entcore.feeder.dictionary.structures.Importer;
import org.entcore.feeder.dictionary.structures.Structure;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonObject;

import static org.entcore.feeder.dictionary.structures.DefaultProfiles.*;

public class StructureImportProcessing extends BaseImportProcessing {

	private final Importer importer = Importer.getInstance();

	protected StructureImportProcessing(String path, Vertx vertx) {
		super(path, vertx);
	}

	@Override
	public void start(final Handler<Message<JsonObject>> handler) {
		createOrUpdateProfiles();
		DefaultFunctions.createOrUpdateFunctions(importer);
		parse(handler, getNextImportProcessing());
	}

	protected ImportProcessing getNextImportProcessing() {
		return new FieldOfStudyImportProcessing(path, vertx);
	}

	private void createOrUpdateProfiles() {
		importer.createOrUpdateProfile(STUDENT_PROFILE);
		importer.createOrUpdateProfile(RELATIVE_PROFILE);
		importer.createOrUpdateProfile(PERSONNEL_PROFILE);
		importer.createOrUpdateProfile(TEACHER_PROFILE);
		importer.createOrUpdateProfile(GUEST_PROFILE);
	}

	@Override
	public String getMappingResource() {
		return "dictionary/mapping/aaf/EtabEducNat.json";
	}

	@Override
	public void process(JsonObject object) {
		Structure structure = importer.createOrUpdateStructure(object);
		if (structure != null) {
			structure.addAttachment();
		}
	}

	@Override
	protected String getFileRegex() {
		return ".*?EtabEducNat_[0-9]{4}\\.xml";
	}

}
