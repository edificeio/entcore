package org.entcore.archive.services;

import fr.wseduc.webutils.Either;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonObject;
import org.entcore.common.events.EventStore;
import org.entcore.common.events.EventStoreFactory;

public interface RepriseService {

    public enum RepriseEvent
    {
        EXPORT_OK, EXPORT_ERROR, IMPORT_OK, IMPORT_ERROR
    }

	public EventStore eventStore = EventStoreFactory.getFactory().getEventStore(RepriseService.class.getSimpleName());

    void launchExportForUsersFromOldPlatform(boolean relativePersonnelFirst);
    void launchImportForUsersFromOldPlatform();
    void imported(String importId, String app, JsonObject rapport);
}
