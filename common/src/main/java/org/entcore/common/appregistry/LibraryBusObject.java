package org.entcore.common.appregistry;

import io.vertx.core.MultiMap;
import io.vertx.core.buffer.Buffer;

public class LibraryBusObject {
    private Buffer cover;
    private MultiMap form;

    LibraryBusObject(MultiMap form, Buffer cover) {
        this.form = form;
        this.cover = cover;
    }

    public Buffer getCover() {
        return cover;
    }

    public MultiMap getForm() {
        return form;
    }
}
