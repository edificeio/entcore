package org.entcore.common.storage;

public class StorageFileAnalyzingException extends RuntimeException {
    public StorageFileAnalyzingException() {
    }

    public StorageFileAnalyzingException(final String message) {
        super(message);
    }

    public StorageFileAnalyzingException(final String message, final Throwable cause) {
        super(message, cause);
    }

    public StorageFileAnalyzingException(final Throwable cause) {
        super(cause);
    }

    public StorageFileAnalyzingException(final String message, final Throwable cause, final boolean enableSuppression, final boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
