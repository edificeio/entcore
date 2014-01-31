package org.entcore.directory.services;

import edu.one.core.infra.Either;
import org.vertx.java.core.Handler;
import org.vertx.java.core.json.JsonObject;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public interface UserService {

	enum UserType {
		Student(Arrays.asList("lastName", "firstName", "surname", "classes", "sector",
				"address", "city", "zipCode", "country", "birthDate", "gender", "level", "email"),
				Arrays.asList("lastName", "firstName", "birthDate"),
				Collections.<String>emptyList(), Collections.<String>emptyList()),
		Teacher(Arrays.asList("lastName", "firstName", "surname", "classes", "title",
				"address", "city", "zipCode", "country", "mobile", "homePhone", "level", "email"),
				Arrays.asList("lastName", "firstName"),
				Collections.<String>emptyList(), Collections.<String>emptyList()),
		Relative(Arrays.asList("lastName", "firstName", "surname", "workPhone", "title", "childrenIds",
				"address", "city", "zipCode", "country", "mobile", "homePhone", "email"),
				Arrays.asList("lastName", "firstName", "childrenIds"),
				Collections.<String>emptyList(), Collections.<String>emptyList()),
		Principal(Arrays.asList("lastName", "firstName", "surname", "classes", "title", "principal",
				"address", "city", "zipCode", "country", "mobile", "homePhone", "level", "email"),
				Arrays.asList("lastName", "firstName"),
				Collections.<String>emptyList(), Collections.<String>emptyList());

		private final List<String> fields;
		private final List<String> requiredFields;
		private final List<String> updateFields;
		private final List<String> updateRequiredFields;

		private UserType(List<String> fields, List<String> requiredFields,
						 List<String> updateFields, List<String> updateRequiredFields) {
			this.fields = fields;
			this.requiredFields = requiredFields;
			this.updateFields = updateFields;
			this.updateRequiredFields = updateRequiredFields;
		}

		public List<String> getFields() {
			return fields;
		}

		public List<String> getRequiredFields() {
			return requiredFields;
		}

		public List<String> getUpdateFields() {
			return updateFields;
		}

		public List<String> getUpdateRequiredFields() {
			return updateRequiredFields;
		}
	}

	void createInClass(String classId, JsonObject user, Handler<Either<String, JsonObject>> result);

}
