package org.entcore.archive.services;

import fr.wseduc.webutils.Either;
import org.entcore.common.user.UserInfos;
import org.vertx.java.core.Handler;

public interface ExportService {

	void export(UserInfos user, String locale, Handler<Either<String, String>> handler);

	void userExportExists(UserInfos user, Handler<Boolean> handler);

	void waitingExport(String exportId, Handler<Boolean> handler);

	void exportPath(String exportId, Handler<Either<String, String>> handler);

	void exported(String exportId, String status, String locale);

	void deleteExport(String exportId);

}
