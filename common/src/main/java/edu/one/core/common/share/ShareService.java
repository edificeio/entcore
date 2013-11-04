package edu.one.core.common.share;

import edu.one.core.infra.Either;
import edu.one.core.infra.security.SecuredAction;
import org.vertx.java.core.Handler;
import org.vertx.java.core.json.JsonObject;

import java.util.List;
import java.util.Map;

public interface ShareService {

	void shareInfos(String userId, String resourceId, Handler<Either<String, JsonObject>> handler);

	void groupShare(String userId, String groupShareId, String resourceId, List<String> actions,
			Handler<Either<String, JsonObject>> handler);

	void userShare(String userId, String userShareId, String resourceId, List<String> actions,
			Handler<Either<String, JsonObject>> handler);

}
