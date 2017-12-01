package org.entcore.admin.services;

import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;

import fr.wseduc.webutils.Either;

public interface AdminService {

	public void quickSearchUsers(String structureId, String input, Handler<Either<String, JsonArray>> handler);
	public void userList(String structureId, Handler<Either<String, JsonArray>> handler);
}
