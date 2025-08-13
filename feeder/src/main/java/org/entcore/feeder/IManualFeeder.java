package org.entcore.feeder;

import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonObject;

public interface IManualFeeder {

	void createStructure(final Message<JsonObject> message);

	void createClass(final Message<JsonObject> message);

	void updateClass(final Message<JsonObject> message);

	void removeClass(final Message<JsonObject> message);

	void createUser(final Message<JsonObject> message);

	void addUser(final Message<JsonObject> message);

	void addUsers(final Message<JsonObject> message);

	void removeUser(final Message<JsonObject> message);

	void removeUsers(final Message<JsonObject> message);

	void updateUser(final Message<JsonObject> message);

	void updateUserLogin(final Message<JsonObject> message);

	void deleteUser(final Message<JsonObject> message);

	void restoreUser(final Message<JsonObject> message);

	void createFunction(final Message<JsonObject> message);

	void deleteFunction(final Message<JsonObject> message);

	void deleteFunctionGroup(Message<JsonObject> message);

	void addUserFunction(final Message<JsonObject> message);

	void addUserHeadTeacherManual(final Message<JsonObject> message);

	void updateUserHeadTeacherManual(final Message<JsonObject> message);

	void createManualSubject(final Message<JsonObject> message);

	void updateManualSubject(final Message<JsonObject> message);

	void deleteManualSubject(final Message<JsonObject> message);

	void addUserDirectionManual(final Message<JsonObject> message);

	void removeUserDirectionManual(final Message<JsonObject> message);

	void removeUserFunction(Message<JsonObject> message);

	void addUserGroup(Message<JsonObject> message);

	void removeUserGroup(Message<JsonObject> message);

	void createOrUpdateTenant(Message<JsonObject> message);

	void createGroup(Message<JsonObject> message);

	void deleteGroup(Message<JsonObject> message);

	void addGroupUsers(Message<JsonObject> message);

	void removeGroupUsers(Message<JsonObject> message);

	void updateEmailGroup(Message<JsonObject> message);

	void structureAttachment(Message<JsonObject> message);

	void structureDetachment(Message<JsonObject> message);

	void updateStructure(final Message<JsonObject> message);

	void relativeStudent(Message<JsonObject> message);

	void unlinkRelativeStudent(Message<JsonObject> message);

}
