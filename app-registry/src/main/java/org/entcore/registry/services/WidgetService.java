package org.entcore.registry.services;

import java.util.List;

import org.vertx.java.core.Handler;
import org.vertx.java.core.json.JsonObject;

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
