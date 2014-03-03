package org.entcore.feeder.aaf;

import org.entcore.feeder.dictionary.structures.DefaultProfiles;
import org.vertx.java.core.Handler;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;

public class StudentImportProcessing2 extends StudentImportProcessing {

	protected StudentImportProcessing2(String path, Vertx vertx) {
		super(path, vertx);
	}

	@Override
	public void start(final Handler<Message<JsonObject>> handler) {
		parse(handler, null);
	}

	@Override
	protected void preCommit() {
		importer.linkRelativeToStructure(DefaultProfiles.RELATIVE_PROFILE_EXTERNAL_ID);
		importer.linkRelativeToClass(DefaultProfiles.RELATIVE_PROFILE_EXTERNAL_ID);
	}

	@Override
	public void process(JsonObject object) {
		String[][] classes = createClasses(object.getArray("classes"));
		String[][] groups = createGroups(object.getArray("groups"));
		JsonArray relative = parseRelativeField(object.getArray("relative"));
		importer.createOrUpdateStudent(object, DefaultProfiles.STUDENT_PROFILE_EXTERNAL_ID,
				null, null, classes, groups, relative, false, true);
	}

}
