package org.entcore.common.bus;

import io.vertx.core.Promise;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.eventbus.Message;

public class MessageUtils
{
    public static void replyWithResults(Message<JsonObject> msg, Future<JsonArray> results)
    {
        if(msg == null)
            return;

        results.onFailure(new Handler<Throwable>()
        {
            @Override
            public void handle(Throwable t)
            {
                JsonObject ret =
                    new JsonObject()
                    .put("status", "error")
                    .put("message", t.getMessage()) // Ceinture
                    .put("error", t.getMessage()); // Bretelles
                msg.reply(ret);
            }
        });

        results.onSuccess(new Handler<JsonArray>()
        {
            @Override
            public void handle(JsonArray res)
            {
                JsonArray allResults = null;
                JsonArray singleResult = null;

                if(res.size() == 0)
                {
                    allResults = new JsonArray().add(res);
                    singleResult = res;
                }
                else
                {
                    Object first = res.getValue(0);
                    if(first instanceof JsonArray)
                    {
                        allResults = res;
                        if(res.size() == 1)
                            singleResult = (JsonArray) first;
                    }
                    else if(first instanceof JsonObject)
                    {
                        allResults = new JsonArray().add(res);
                        singleResult = res;
                    }
                    else
                        allResults = res;
                }

                JsonObject ret =
                    new JsonObject()
                    .put("status", "ok")
                    .put("results", allResults)
                    .put("result", singleResult);
                msg.reply(ret);
            }
        });
    }
}