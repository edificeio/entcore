package org.entcore.common.http.filter;

import org.entcore.common.neo4j.Neo4j;
import org.entcore.common.user.DefaultFunctions;
import org.entcore.common.user.UserInfos;

import io.vertx.core.Handler;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

public class AdminUpdateFilter {

    private Neo4j neo4j = Neo4j.getInstance();

    public void checkADMCUpdate(final HttpServerRequest request, UserInfos user, final Handler<Boolean> handler) {
        checkADMCUpdate(request, user, true, handler);
    }

    public void checkADMCUpdate(final HttpServerRequest request, UserInfos user, boolean pauseresume, final Handler<Boolean> handler) {
        final String id = request.params().get("userId");
		if (id == null || id.trim().isEmpty()) {
			handler.handle(false);
			return;
        }
        checkADMCUpdate(request, user, new JsonArray().add(id), pauseresume, handler);
    }

    public void checkADMCUpdate(final HttpServerRequest request, UserInfos user, JsonArray userIds,
            final boolean pauseresume, final Handler<Boolean> handler) {
        checkADMCUpdate(request, user, userIds, pauseresume, false, handler);
    }

    public void checkADMCUpdate(final HttpServerRequest request, UserInfos user, JsonArray userIds,
            final boolean pauseresume, boolean login, final Handler<Boolean> handler) {
		if (user.getFunctions() != null && user.getFunctions().containsKey(DefaultFunctions.SUPER_ADMIN)) {
			handler.handle(true);
			return;
		}

        if (pauseresume) {
            request.pause();
        }
		final JsonObject params = new JsonObject().put("userIds", userIds);
		if (user.getFunctions() != null && user.getFunctions().containsKey(DefaultFunctions.ADMIN_LOCAL)) {
			params.put("functions", new JsonArray().add(DefaultFunctions.SUPER_ADMIN));
		} else {
			params.put("functions", new JsonArray().add(DefaultFunctions.SUPER_ADMIN).add(DefaultFunctions.ADMIN_LOCAL));
        }
        final String userAttr = login ? "login" : "id";
		final String query =
			"MATCH (u:User)-[:HAS_FUNCTION]->(f:Function) " +
			"WHERE u." + userAttr + " IN {userIds} AND f.externalId IN {functions} " +
			"RETURN count(*) <> 0 as containsADMC;";
		neo4j.execute(query, params, r -> {
            JsonArray res = r.body().getJsonArray("result");
            if (pauseresume) {
                request.resume();
            }
            handler.handle(
                    "ok".equals(r.body().getString("status")) &&
                            res.size() == 1 && !(res.getJsonObject(0)).getBoolean("containsADMC", true)
            );
		});
    }

}
