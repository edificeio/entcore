package org.entcore.common.messaging;

public class MessagingException extends RuntimeException {
    public MessagingException() {
    }

    public MessagingException(final String message) {
        super(message);
    }

    public MessagingException(final String message, final Throwable cause) {
        super(message, cause);
    }

    public MessagingException(final Throwable cause) {
        super(cause);
    }

    public MessagingException(final String message, final Throwable cause, final boolean enableSuppression, final boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
