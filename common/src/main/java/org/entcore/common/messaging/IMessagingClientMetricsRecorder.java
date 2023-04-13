package org.entcore.common.messaging;

/**
 * Records exchanges made through {@code IMessagingClient}.
 */
public interface IMessagingClientMetricsRecorder {
    /**
     * Called when messages were successfully sent via a client.
     * @param numberOfSuccessfulMessages
     * @param duration The time it took (in milliseconds) to send the messages
     */
    void onSendMessageSuccess(int numberOfSuccessfulMessages, final long duration);

    /**
     * Called when an error occurred while sending messages via a client.
     * @param numberOfFailedMessages
     * @param duration The time it took (in milliseconds) to send the messages
     */
    void onSendMessageFailure(int numberOfFailedMessages, final long duration);

    /**
     * Called when a client has successfully treated a message
     * @param processingDuration The time it took to process the message
     * @param lifetime The time between the creation of the message and the time it was finally processed
     */
    void onMessageProcessSuccessfully(final long processingDuration, final long lifetime);
    /**
     * Called when a client encountered an error while processing a message
     * @param processingDuration The time it took to process the message
     * @param lifetime The time between the creation of the message and the time it was finally processed
     */
    void onMessageProcessError(final long processingDuration, final long lifetime);

    static final NoopMessagingClientMetricsRecorder noop = new NoopMessagingClientMetricsRecorder();

    static class NoopMessagingClientMetricsRecorder implements IMessagingClientMetricsRecorder {
        private NoopMessagingClientMetricsRecorder() {}
        @Override
        public void onSendMessageSuccess(final int numberOfSuccessfulMessages, final long duration) {
            // Let this implementation empty
        }

        @Override
        public void onSendMessageFailure(final int numberOfFailedMessages, final long duration) {
            // Let this implementation empty
        }

        @Override
        public void onMessageProcessSuccessfully(final long processingDuration, final long lifetime) {
            // Let this implementation empty
        }

        @Override
        public void onMessageProcessError(final long processingDuration, final long lifetime) {
            // Let this implementation empty
        }
    }
}
