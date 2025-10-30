package org.entcore.common.user.position;

public class ExportResourceResult {
    private final boolean ok;
    private final String exportPath;

    public ExportResourceResult(boolean ok, String exportPath) {
        this.ok = ok;
        this.exportPath = exportPath;
    }

    public boolean isOk() {
        return ok;
    }

    public String getExportPath() {
        return exportPath;
    }
}
