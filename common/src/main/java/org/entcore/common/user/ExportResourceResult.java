package org.entcore.common.user;

public class ExportResourceResult {
    private final boolean ok;
    private final String exportPath;
    public static final ExportResourceResult KO = new ExportResourceResult(false, null);

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
