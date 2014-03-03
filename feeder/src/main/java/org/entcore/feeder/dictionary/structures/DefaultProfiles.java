package org.entcore.feeder.dictionary.structures;

import org.vertx.java.core.json.JsonObject;

public final class DefaultProfiles {

	private DefaultProfiles() {}

	public static final String TEACHER_PROFILE_EXTERNAL_ID = "PROFILE_TEACHER";
	public static final JsonObject TEACHER_PROFILE = new JsonObject()
			.putString("externalId", TEACHER_PROFILE_EXTERNAL_ID)
			.putString("name", "Teacher");

	public static final String RELATIVE_PROFILE_EXTERNAL_ID = "PROFILE_RELATIVE";
	public static final JsonObject RELATIVE_PROFILE = new JsonObject()
			.putString("externalId", RELATIVE_PROFILE_EXTERNAL_ID)
			.putString("name", "Relative");

	public static final String STUDENT_PROFILE_EXTERNAL_ID = "PROFILE_STUDENT";
	public static final JsonObject STUDENT_PROFILE = new JsonObject()
			.putString("externalId", STUDENT_PROFILE_EXTERNAL_ID)
			.putString("name", "Student");

	public static final String PERSONNEL_PROFILE_EXTERNAL_ID = "PROFILE_PERSONNEL";
	public static final JsonObject PERSONNEL_PROFILE = new JsonObject()
			.putString("externalId", PERSONNEL_PROFILE_EXTERNAL_ID)
			.putString("name", "Personnel");

}
