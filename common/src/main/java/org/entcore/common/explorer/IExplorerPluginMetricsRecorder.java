package org.entcore.common.explorer;

/**
 * Translates events coming from the plugin into metrics.
 */
public interface IExplorerPluginMetricsRecorder {
    void onSendMessageSuccess(int numberOfSuccessfulMessages);
    void onSendMessageFailure(int numberOfFailedMessages);
    class NoopExplorerPluginMetricsRecorder implements IExplorerPluginMetricsRecorder {
        public static final NoopExplorerPluginMetricsRecorder instance = new NoopExplorerPluginMetricsRecorder();
        @Override
        public void onSendMessageSuccess(int numberOfSuccessfulMessages) {

        }

        @Override
        public void onSendMessageFailure(int numberOfFailedMessages) {

        }
    }
}
