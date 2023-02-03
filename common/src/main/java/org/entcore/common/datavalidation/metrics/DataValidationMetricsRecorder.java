package org.entcore.common.datavalidation.metrics;

/**
 * Translates events coming from the data validation (email+mobile) and MFA workflows into metrics.
 */
public interface DataValidationMetricsRecorder {
    void onEmailCodeGenerated();
    void onMobileCodeGenerated();
    void onMfaCodeGenerated();

    void onEmailCodeConsumed();
    void onMobileCodeConsumed();
    void onMfaCodeConsumed();

    class NoopRecorder implements DataValidationMetricsRecorder {
        public static final NoopRecorder instance = new NoopRecorder();
        @Override public void onEmailCodeGenerated() {}
        @Override public void onMobileCodeGenerated() {};
        @Override public void onMfaCodeGenerated() {};
        @Override public void onEmailCodeConsumed() {};
        @Override public void onMobileCodeConsumed() {};
        @Override public void onMfaCodeConsumed() {};
    }
}
