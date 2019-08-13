package org.entcore.common.appregistry;

import io.vertx.core.MultiMap;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.eventbus.MessageCodec;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.util.Map;

public class LibraryBusObjectCodec implements MessageCodec<LibraryBusObject, LibraryBusObject> {

    @Override
    public void encodeToWire(Buffer buffer, LibraryBusObject libraryBusObject) {
        Buffer cover = libraryBusObject.getCover();
        MultiMap form = libraryBusObject.getForm();

        JsonObject jsonForm = new JsonObject();
        for (Map.Entry<String, String> entry : form.entries()) {
            jsonForm.put(entry.getKey(), entry.getValue());
        }

        String strForm = jsonForm.encode();
        buffer.appendInt(strForm.getBytes().length);
        buffer.appendString(strForm);
        buffer.appendInt(cover.length());
        buffer.appendBuffer(cover);
    }

    @Override
    public LibraryBusObject decodeFromWire(int position, Buffer buffer) {
        int pos = position;
        int formLength = buffer.getInt(pos);
        String strForm = buffer.getString(pos += 4, pos += formLength);
        int coverLength = buffer.getInt(pos);
        Buffer cover = buffer.getBuffer(pos += 4, pos += coverLength);

        JsonObject jsonForm = new JsonObject(strForm);
        MultiMap form = MultiMap.caseInsensitiveMultiMap();
        jsonForm.fieldNames().forEach(field -> {
            Object fieldValue = jsonForm.getValue(field);
            if (fieldValue instanceof JsonArray) {
                ((JsonArray) fieldValue).getList().forEach(fieldValueElement -> {
                    form.add(field, (String) fieldValueElement);
                });
            } else {
                form.add(field, (String) fieldValue);
            }
        });

        return new LibraryBusObject(form, cover);
    }

    @Override
    public LibraryBusObject transform(LibraryBusObject libraryBusObject) {
        return libraryBusObject;
    }

    @Override
    public String name() {
        return "LibraryBusObjectCodec";
    }

    @Override
    public byte systemCodecID() {
        return -1;
    }
}
