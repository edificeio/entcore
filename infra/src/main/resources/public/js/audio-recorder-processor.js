/**
 * audio-recorder-processor.js
 * Processor for the Audio Recorder Component (see edifice-ui/packages/react/src/multimedia/AudioRecorder/AudioRecorder.tsx).
 * This processor sends the recorded audio inputs to the AudioWorkletNode.
 *
 * For more information on Audio Processors, please see https://developer.mozilla.org/en-US/docs/Web/API/AudioWorkletProcessor
 */
class AudioRecorderProcessor extends AudioWorkletProcessor {
  process(inputs, outputs, parameters) {
    this.port.postMessage({ inputs });
    return true;
  }
}

registerProcessor("audio-recorder-processor", AudioRecorderProcessor);
