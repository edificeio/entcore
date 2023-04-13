package org.entcore.common.storage;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class XSSScan {
    private final String fileId;
    private final String mimeType;

    @JsonCreator
    public XSSScan(@JsonProperty("fileId") final String fileId,
                   @JsonProperty("mimeType") final String mimeType) {
        this.fileId = fileId;
        this.mimeType = mimeType;
    }

    public String getFileId() {
        return fileId;
    }

    public String getMimeType() {
        return mimeType;
    }
}
