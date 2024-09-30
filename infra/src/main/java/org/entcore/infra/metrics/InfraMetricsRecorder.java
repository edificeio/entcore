package org.entcore.infra.metrics;

public interface InfraMetricsRecorder {
  class NoopInfraMetricsRecorder implements InfraMetricsRecorder {
    public static final InfraMetricsRecorder instance = new NoopInfraMetricsRecorder();
  }
}
