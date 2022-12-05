package org.entcore.common.explorer;

public class IdAndVersion {
    private final String id;
    private final long version;

    public IdAndVersion(String id, long version) {
        this.id = id;
        this.version = version;
    }

    public String getId() {
        return id;
    }

    public long getVersion() {
        return version;
    }
}
