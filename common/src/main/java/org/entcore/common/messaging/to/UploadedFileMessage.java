package org.entcore.common.messaging.to;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class UploadedFileMessage extends ClientMessage {
    private final String id;
    private final String name;
    private final String filename;
    private final String contentType;
    private final String contentTransferEncoding;
    private final String charset;
    private final long size;
    private final String storage;

    @JsonCreator
    public UploadedFileMessage(@JsonProperty("id") final String id,
                               @JsonProperty("name") final String name,
                               @JsonProperty("filename") final String filename,
                               @JsonProperty("contentType") final String contentType,
                               @JsonProperty("contentTransferEncoding") final String contentTransferEncoding,
                               @JsonProperty("charset") final String charset,
                               @JsonProperty("size") final long size,
                               @JsonProperty("creationTime") final long creationTime,
                               @JsonProperty("storage") final String storage) {
        super("na", creationTime);
        this.id = id;
        this.name = name;
        this.filename = filename;
        this.contentType = contentType;
        this.contentTransferEncoding = contentTransferEncoding;
        this.charset = charset;
        this.size = size;
        this.storage = storage;
    }

    public String getName() {
        return name;
    }

    public String getFilename() {
        return filename;
    }

    public String getContentType() {
        return contentType;
    }

    public String getContentTransferEncoding() {
        return contentTransferEncoding;
    }

    public String getCharset() {
        return charset;
    }

    public long getSize() {
        return size;
    }

    public String getStorage() {
        return storage;
    }

    public String getId() {
        return id;
    }
}
