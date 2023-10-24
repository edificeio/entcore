/*
 * Copyright © "Open Digital Education", 2018
 *
 * This program is published by "Open Digital Education".
 * You must indicate the name of the software and the company in any production /contribution
 * using the software and indicate on the home page of the software industry in question,
 * "powered by Open Digital Education" with a reference to the website: https://opendigitaleducation.com/.
 *
 * This program is free software, licensed under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation, version 3 of the License.
 *
 * You can redistribute this application and/or modify it since you respect the terms of the GNU Affero General Public License.
 * If you modify the source code and then use this modified source code in your creation, you must make available the source code of your modifications.
 *
 * You should have received a copy of the GNU Affero General Public License along with the software.
 * If not, please see : <http://www.gnu.org/licenses/>. Full compliance requires reading the terms of this license and following its directives.
 */

package org.entcore.common.notification;

import fr.wseduc.webutils.Either;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;
import org.entcore.common.neo4j.Neo4j;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.entcore.common.utils.HtmlUtils;


import static fr.wseduc.webutils.Utils.handlerToAsyncHandler;
import static org.entcore.common.neo4j.Neo4jResult.validUniqueResultHandler;

public class NotificationUtils {
    private static final String USERBOOK_ADDRESS = "userbook.preferences";

    private static final int NB_CHARACTERS_IN_TEXT_NOTIFICATION = 150;


    public static void getUsersPreferences(EventBus eb, JsonArray userIds, String fields, final Handler<JsonArray> handler){
        eb.request(USERBOOK_ADDRESS, new JsonObject()
                .put("action", "get.userlist")
                .put("application", "timeline")
                .put("additionalMatch", ", u-[:IN]->(g:Group)-[:AUTHORIZED]->(r:Role)-[:AUTHORIZE]->(act:WorkflowAction) ")
                .put("additionalWhere", "AND act.name = \"org.entcore.timeline.controllers.TimelineController|mixinConfig\" ")
                .put("additionalCollectFields", ", " + fields)
                .put("userIds", userIds), handlerToAsyncHandler(new Handler<Message<JsonObject>>() {
            public void handle(Message<JsonObject> event) {
                if (!"error".equals(event.body().getString("status"))) {
                    handler.handle(event.body().getJsonArray("results"));
                } else {
                    handler.handle(null);
                }
            }
        }));
    }

    public static void putFcmToken(String userId, String fcmToken, Handler<Either<String, JsonObject>> handler){
        final JsonObject params = new JsonObject().put("userId", userId).put("fcmToken", fcmToken);

        String query = "MATCH (u:User {id: {userId}}) MERGE (u)-[:PREFERS]->(uac:UserAppConf)" +
                "ON CREATE SET uac.fcmTokens = [{fcmToken}] " +
                "ON MATCH SET uac.fcmTokens = FILTER(token IN coalesce(uac.fcmTokens, []) WHERE token <> {fcmToken}) + {fcmToken}";

        Neo4j.getInstance().execute(query, params, validUniqueResultHandler(handler));

    }

    public static void getFcmTokensByUser(String userId, final Handler<Either<String, JsonArray>> handler){
        final JsonObject params = new JsonObject().put("userId", userId);

        String query = "MATCH (u:User {id:{userId}})-[:PREFERS]->(uac:UserAppConf)"
                +" RETURN uac.fcmTokens AS tokens";

        Neo4j.getInstance().execute(query, params, validUniqueResultHandler(new Handler<Either<String, JsonObject>>() {
            @Override
            public void handle(Either<String, JsonObject> event) {
                if(event.isRight()){
                        JsonArray result = event.right().getValue().getJsonArray("tokens");
                    if (result == null)
                        result = new JsonArray();
                    handler.handle(new Either.Right<String, JsonArray>(result));
                }else {
                    handler.handle(new Either.Left<String, JsonArray>(event.left().getValue()));
                }
            }
        }));
    }

    public static void deleteFcmToken(String userId, String fcmToken, Handler<Either<String, JsonObject>> handler){
        final JsonObject params = new JsonObject().put("userId", userId).put("fcmToken", fcmToken);

        final String query =
                "MATCH (u:User {id: {userId}})-[:PREFERS]->(uac:UserAppConf) " +
                "SET uac.fcmTokens = FILTER(token IN coalesce(uac.fcmTokens, []) WHERE token <> {fcmToken})";

        Neo4j.getInstance().execute(query, params, validUniqueResultHandler(handler));

    }

    public static void deleteFcmTokens(JsonArray userIds, Handler<Either<String, JsonObject>> handler){
        final JsonObject params = new JsonObject().put("userIds", userIds);

        final String query =
                "MATCH (u:User)-[:PREFERS]->(uac:UserAppConf) " +
                "WHERE u.id IN {userIds} " +
                "SET uac.fcmTokens = null ";

        Neo4j.getInstance().execute(query, params, validUniqueResultHandler(handler));

    }

    public static void getFcmTokensByUsers(JsonArray userIds,final Handler<Either<String, JsonArray>> handler){
        final JsonObject params = new JsonObject().put("userIds", userIds);

        String query = "MATCH (u:User)-[:PREFERS]->(uac:UserAppConf) WHERE u.id IN {userIds} " +
                " UNWIND(uac.fcmTokens) as token WITH DISTINCT token RETURN collect(token) as tokens";

        Neo4j.getInstance().execute(query, params, validUniqueResultHandler(new Handler<Either<String, JsonObject>>() {
            @Override
            public void handle(Either<String, JsonObject> event) {
                if(event.isRight()){
                    JsonArray result = event.right().getValue().getJsonArray("tokens");
                    if (result == null)
                        result = new JsonArray();
                    handler.handle(new Either.Right<String, JsonArray>(result));
                }else {
                    handler.handle(new Either.Left<String, JsonArray>(event.left().getValue()));
                }
            }
        }));
    }

    /**
     * Use {@link #jsonContentToPreview(JsonObject)} instead.
     */
    @Deprecated
    public static JsonObject htmlContentToPreview(String htmlContent){
        JsonObject preview =  new JsonObject();
        String text = HtmlUtils.extractFormatText(htmlContent, 4, 150);
        if(text.length() > 146)
            text = text.substring(0, 146) + "...";
        preview.put("text", text);
        preview.put("images", HtmlUtils.getAllImagesSrc(htmlContent)); // retro-compatibility for ode-mobile-framework < 1.2
        preview.put("medias", HtmlUtils.extractMedias(htmlContent));
        return preview;
    }
 
    /**
     * @param jsonContent Content whose preview we want
     * @return An object containing the following fields :
     * <ul>
     *   <li>text, the plain text</li>
     *   <li>images, list of images in this content</li>
     *   <li>medias, list of all media (image, video, iframe, audio and attachments) in this content</li>
     * </ul>
     */
    public static JsonObject jsonContentToPreview(final JsonObject jsonContent) {
        return new JsonObject()
          .put("text", getText(jsonContent, NB_CHARACTERS_IN_TEXT_NOTIFICATION + 1))
          .put("images", getAllImagesSrc(jsonContent))
          .put("medias", extractMedias(jsonContent));
    }

    private static JsonArray extractMedias(JsonObject jsonContent) {
        final JsonArray medias = new JsonArray();
        final String type = jsonContent.getString("type");
        if(type != null) {
            switch (type) {
                case "custom-image":
                case "audio":
                case "iframe":
                case "video":
                    final JsonObject media = camelCaseToKebabCaseKeys(jsonContent.getJsonObject("attrs"));
                    media.put("type", "custom-image".equals(type) ? "image" : type);
                    if(media.containsKey("is-captation")) {
                        media.put("document-is-captation", media.remove("is-captation"));
                    }
                    medias.add(media);
                    break;
                case "attachments":
                    final JsonArray links = jsonContent.getJsonObject("attrs").getJsonArray("links");
                    if (links != null) {
                        for (Object link : links) {
                            if(link instanceof JsonObject) {
                                final JsonObject linkElt = (JsonObject) link;
                                medias.add(new JsonObject()
                                  .put("type", "attachment")
                                  .put("src", linkElt.getString("href"))
                                  .put("name", linkElt.getString("name")));
                            }
                        }
                    }
                    break;
                default:
                    final JsonArray children = jsonContent.getJsonArray("content");
                    if (children != null) {
                        for (Object child : children) {
                            if(child instanceof JsonObject) {
                                medias.addAll(extractMedias((JsonObject) child));
                            }
                        }
                    }
            }
        }
        return medias;
    }

    /**
     * Creates a copy of the specified object but transform camelCased keys to their kebab cased versions.
     * @param camelCasedObject Camel cased keys object
     * @return Copy of the specified object but transform camelCased keys to their kebab cased versions.
     */
    public static JsonObject camelCaseToKebabCaseKeys(JsonObject camelCasedObject) {
        final JsonObject kebabCasedObject;
        if(camelCasedObject == null) {
            kebabCasedObject = null;
        } else {
            kebabCasedObject = new JsonObject();
            camelCasedObject.stream().forEach(entry -> {
                final String originalKey = entry.getKey();
                String kebabCasedKey = originalKey.replaceAll("([a-z])([A-Z])", "$1-$2");
                if(!kebabCasedKey.equals(originalKey)) {
                    kebabCasedKey = kebabCasedKey.toLowerCase();
                }
                kebabCasedObject.put(kebabCasedKey, entry.getValue());
            });
        }
        return kebabCasedObject;
    }

    private static JsonArray getAllImagesSrc(JsonObject jsonContent) {
        final JsonArray images = new JsonArray();
        final String type = jsonContent.getString("type");
        if("custom-image".equals(type)) {
            images.add(jsonContent.getJsonObject("attrs").getString("src"));
        } else {
            final JsonArray children = jsonContent.getJsonArray("content");
            if(children != null) {
                for (Object child : children) {
                    if(child instanceof JsonObject) {
                        images.addAll(getAllImagesSrc((JsonObject) child));
                    }
                }
            }
        }
        return images;
    }

    private static String getText(final JsonObject jsonContent, final int nbCharsToAdd) {
        final StringBuilder sbuffer = new StringBuilder();
        final String type = jsonContent.getString("type");
        if("text".equals(type)) {
            sbuffer.append(jsonContent.getString("text"));
        } else {
            final JsonArray children = jsonContent.getJsonArray("content");
            if(children != null) {
                int remainingChars = nbCharsToAdd;
                for(int i = 0; i < children.size() && remainingChars > 0; i ++) {
                    final JsonObject child = children.getJsonObject(i);
                    final String childPart = getText(child, nbCharsToAdd);
                    remainingChars -= childPart.length();
                    sbuffer.append(childPart);
                }
            }
        }
        if(sbuffer.length() > NB_CHARACTERS_IN_TEXT_NOTIFICATION) {
            sbuffer.setCharAt(NB_CHARACTERS_IN_TEXT_NOTIFICATION - 1, '…');
            sbuffer.setLength(NB_CHARACTERS_IN_TEXT_NOTIFICATION);
        }
        return sbuffer.toString();
    }
}
