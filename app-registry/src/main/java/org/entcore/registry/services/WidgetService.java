/*
 * Copyright © WebServices pour l'Éducation, 2016
 *
 * This file is part of ENT Core. ENT Core is a versatile ENT engine based on the JVM.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation (version 3 of the License).
 *
 * For the sake of explanation, any module that communicate over native
 * Web protocols, such as HTTP, with ENT Core is outside the scope of this
 * license and could be license under its own terms. This is merely considered
 * normal use of ENT Core, and does not fall under the heading of "covered work".
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 */

package org.entcore.registry.services;

import java.util.List;

import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;

import fr.wseduc.webutils.Either;

public interface WidgetService {

	public void createWidget(String applicationId, JsonObject widget, Handler<Either<String, JsonObject>> handler);
	public void listWidgets(Handler<Either<String, JsonObject>> handler);
	public void getWidgetInfos(String widgetId, String structureId, Handler<Either<String, JsonObject>> handler);
	public void deleteWidget(String widgetId, Handler<Either<String, JsonObject>> handler);
	public void toggleLock(String widgetId, Handler<Either<String, JsonObject>> handler);
	public void linkWidget(String widgetId, List<String> groupIds, Handler<Either<String, JsonObject>> handler);
	public void unlinkWidget(String widgetId, List<String> groupIds, Handler<Either<String, JsonObject>> handler);
	public void setMandatory(String widgetId, List<String> groupIds, Handler<Either<String, JsonObject>> handler);
	public void removeMandatory(String widgetId, List<String> groupIds, Handler<Either<String, JsonObject>> handler);
	public void massAuthorize(String widgetId, String structureId, List<String> profiles, Handler<Either<String, JsonObject>> handler);
	public void massUnauthorize(String widgetId, String structureId, List<String> profiles, Handler<Either<String, JsonObject>> handler);
	public void massSetMandatory(String widgetId, String structureId, List<String> profiles, Handler<Either<String, JsonObject>> handler);
	public void massRemoveMandatory(String widgetId, String structureId, List<String> profiles, Handler<Either<String, JsonObject>> handler);

}
