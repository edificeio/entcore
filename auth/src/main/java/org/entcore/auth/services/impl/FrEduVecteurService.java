/*
 * Copyright © WebServices pour l'Éducation, 2015
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
package org.entcore.auth.services.impl;

import fr.wseduc.webutils.Either;
import org.entcore.auth.services.SamlVectorService;
import org.entcore.common.neo4j.Neo4j;
import org.entcore.common.neo4j.Neo4jResult;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import static org.entcore.common.neo4j.Neo4jResult.validResultHandler;

public class FrEduVecteurService implements SamlVectorService {


    private final Neo4j neo4j;

    public FrEduVecteurService(Neo4j neo4j) {
        this.neo4j = neo4j;
    }

    public void getVectors(final String userId, final Handler<Either<String, JsonArray>> handler) {
        String queryGetUserProfile ="MATCH (u:User) WHERE u.id = {userId} RETURN u.profiles";
        neo4j.execute(queryGetUserProfile, new JsonObject().put("userId", userId), Neo4jResult.validUniqueResultHandler(new Handler<Either<String, JsonObject>>() {
            @Override
            public void handle(final Either<String, JsonObject> event) {
                if(event.isRight()) {
                    JsonObject value = event.right().getValue();
                    if(value == null) {
                        handler.handle(new Either.Left<String, JsonArray>("error : user not found"));
                    } else {
                        JsonArray profiles = value.getJsonArray("u.profiles");
                        if (profiles != null && profiles.size() > 0) {
                            if (profiles.contains("Student")) {
                                // Get student vectors for saml response : "4|"+u.lastName+"|"+u.firstName+"|"+u.externalId+"|"+s.UAI
                                String query = "MATCH u-[:IN]->()-[:DEPENDS]->(s:Structure) " +
                                        "WHERE u.id = {userId} " +
                                        "RETURN DISTINCT '4|'+u.lastName+'|'+u.firstName+'|'+u.attachmentId+'|'+s.UAI as FrEduVecteur";

                                neo4j.execute(query, new JsonObject().put("userId", userId), validResultHandler(handler));
                            } else if (profiles.contains("Relative")) {
                                // Get parent vectors for saml response : '2|'+u.lastName+'|'+u.firstName+'|'+ child.externalId+'|'+s.UAI"
                                String query = "MATCH (child: User)-[:RELATED]->u-[:IN]->()-[:DEPENDS]->(s:Structure) " +
                                        "WHERE u.id = {userId} " +
                                        "RETURN DISTINCT '2|'+u.lastName+'|'+u.firstName+'|'+ child.attachmentId+'|'+s.UAI as FrEduVecteur";

                                neo4j.execute(query, new JsonObject().put("userId", userId), validResultHandler(handler));
                            } else {
                                // We return null for others profiles
                                handler.handle(new Either.Left<String, JsonArray>("error : profil not supported"));
                            }
                        }
                    }
                } else {
                    handler.handle(new Either.Left<String, JsonArray>("error : user or profile not found"));
                }
            }
        }));
    }
}
