package org.entcore.cas.services;

import fr.wseduc.cas.async.Handler;
import fr.wseduc.cas.entities.AuthCas;
import fr.wseduc.cas.entities.User;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.entcore.common.neo4j.Neo4j;

import java.util.ArrayList;
import java.util.List;

import static fr.wseduc.webutils.Utils.handlerToAsyncHandler;
import static org.entcore.common.neo4j.Neo4jResult.validResultHandler;

public class TurboselfParentRegisteredService extends DefaultRegisteredService {

    private static final String USERID = "userId";
    private static final String TYPE = "type";
    private static final String ID = "id";
    private static final String IDS = "ids";
    private static final String STRUCTURENODES = "structureNodes";
    private static final String UAI = "UAI";
    private static final String STRUCTURES = "structures";
    private static final String FIRSTNAME = "firstName";
    private static final String LASTNAME = "lastName";
    private static final String RELATIVE = "Relative";
    private static final String CHILDREN = "children";
    private static final String UAI_LIST = "UAI_List";


    @Override
    public void getUser(final AuthCas authCas, final String service, final Handler<User> userHandler) {
        final String userId = authCas.getUser();
        JsonObject jo = new JsonObject();

        jo.put("action", directoryAction).put(USERID, userId);
        eb.send("directory", jo, handlerToAsyncHandler(event -> {
            JsonObject res = event.body().getJsonObject("result");

            if ("ok".equals(event.body().getString("status")) && res != null) {
                if(res.getJsonArray(TYPE).contains(RELATIVE)){
                    prepareRelative(authCas, res, userId, service, userHandler);
                } else {
                    userHandler.handle(null);
                }
            } else {
                userHandler.handle(null);
            }

        }));
    }


    private JsonArray extractUais(JsonObject res){
        JsonArray uais = new JsonArray();
        for (Object o : res.getJsonArray(STRUCTURENODES, new JsonArray())){
            if (!(o instanceof JsonObject)) continue;
            JsonObject structure = (JsonObject) o;
            if (structure.containsKey(UAI)) {
                uais.add(structure.getString(UAI));
            }
        }
        return uais;
    }


    private List<String> extractChildrenIds(JsonObject res){
        List<String> childrenIds = new ArrayList<>();
        for (Object o : res.getJsonArray(CHILDREN, new JsonArray())){
            if (!(o instanceof JsonObject)) continue;
            JsonObject child = (JsonObject) o;
            if (child.containsKey(ID)) {
                String childId = child.getString(ID);
                childrenIds.add(childId);
            }
        }
        return childrenIds;
    }
    private void prepareRelative(final AuthCas authCas, JsonObject res, final String userId, final String service,  final Handler<User> userHandler){
        String lastName = res.getString(LASTNAME);
        String firstName = res.getString(FIRSTNAME);
        JsonArray uais = extractUais(res);
        List<String> childrenIds = extractChildrenIds(res);

        User user = new User();
        JsonObject smallData = new JsonObject().put(principalAttributeName, res.getString(principalAttributeName));
        smallData.put(FIRSTNAME, firstName);
        smallData.put(LASTNAME, lastName);
        smallData.put(STRUCTURES, uais);
        extractChildrenData(childrenIds)
                .onSuccess(children -> {
                    smallData.put(CHILDREN, children);

                    prepareUser(user, userId, service, smallData);
                    userHandler.handle(user);
                    createStatsEvent(authCas, res, service);
                })
                .onFailure(err -> {
                    log.error("Failed to retrieve children data", err);
                    userHandler.handle(null);
                });
    }

    private Future<JsonArray> extractChildrenData(List<String> childrenIds){
        Promise<JsonArray> promise = Promise.promise();

        getChildrenData(childrenIds).onSuccess( childrenData -> {
            JsonArray children = new JsonArray();
            childrenData.forEach(element -> {
                JsonObject child = (JsonObject) element;
                child.put(STRUCTURES, child.getJsonArray(UAI_LIST));
                child.remove(UAI_LIST);
                children.add(child);
            });

            promise.complete(children);
        });

        return promise.future();
    }


    private Future<JsonArray> getChildrenData(List<String> childId){
        Promise<JsonArray> promise = Promise.promise();


        String query = "MATCH (u:User)-[:ADMINISTRATIVE_ATTACHMENT]->(s:Structure)\n" +
                "WHERE u.id IN {ids} \n" +
                "RETURN u.id AS UserId, \n" +
                "       u.firstName AS FirstName, \n" +
                "       u.lastName AS LastName, \n" +
                "       u.birthDate AS BirthDate, \n" +
                "       u.accommodation AS Accommodation, \n" +
                "       u.ine AS INE, \n" +
                "       collect(s.UAI) AS UAI_List\n";


        Neo4j.getInstance().execute(query, new JsonObject().put(IDS, childId), validResultHandler(event -> {
            if (event.isLeft()) promise.fail(event.left().getValue());
            else promise.complete(event.right().getValue());
        }));
        return promise.future();
    }
}