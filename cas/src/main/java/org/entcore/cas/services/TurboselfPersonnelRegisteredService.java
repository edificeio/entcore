package org.entcore.cas.services;

import fr.wseduc.cas.async.Handler;
import fr.wseduc.cas.entities.AuthCas;
import fr.wseduc.cas.entities.User;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import static fr.wseduc.webutils.Utils.handlerToAsyncHandler;

public class TurboselfPersonnelRegisteredService extends DefaultRegisteredService {

    private static final String USERID = "userId";
    private static final String TYPE = "type";
    private static final String ID = "id";
    private static final String STRUCTURENODES = "structureNodes";
    private static final String UAI = "UAI";
    private static final String STRUCTURES = "structures";
    private static final String FIRSTNAME = "firstName";
    private static final String LASTNAME = "lastName";


    @Override
    public void getUser(final AuthCas authCas, final String service, final Handler<User> userHandler) {
        final String userId = authCas.getUser();
        JsonObject jo = new JsonObject();

        jo.put("action", directoryAction).put(USERID, userId);
        eb.send("directory", jo, handlerToAsyncHandler(event -> {
            JsonObject res = event.body().getJsonObject("result");

            if ("ok".equals(event.body().getString("status")) && res != null) {
                if(res.getJsonArray(TYPE).contains("Personnel")){
                    preparePersonnel(authCas, res, userId, service, userHandler);
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

    private void preparePersonnel(final AuthCas authCas, JsonObject res, final String userId, final String service,  final Handler<User> userHandler){
        String id = res.getString(ID);
        String lastName = res.getString(LASTNAME);
        String firstName = res.getString(FIRSTNAME);
        JsonArray uais = extractUais(res);

        User user = new User();
        JsonObject smallData = new JsonObject().put(principalAttributeName, res.getString(principalAttributeName));
        smallData.put(FIRSTNAME, firstName);
        smallData.put(LASTNAME, lastName);
        smallData.put(STRUCTURES, uais);
        prepareUser(user, userId, service, smallData);
        userHandler.handle(user);
        createStatsEvent(authCas, res, service);
    }
}
