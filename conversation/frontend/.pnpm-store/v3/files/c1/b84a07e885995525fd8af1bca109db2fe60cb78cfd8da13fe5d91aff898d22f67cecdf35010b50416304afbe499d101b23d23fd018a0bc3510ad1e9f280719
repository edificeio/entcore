import { Node } from "@tiptap/core";
class SR_Node extends Node {
  constructor() {
    super(), this.isStarted = !1;
  }
  static create(config) {
    return Node.create(config);
  }
}
const SpeechRecognition = SR_Node.create({
  name: "SpeechRecognition",
  addOptions() {
    return {
      lang: "fr-FR"
    };
  },
  onCreate() {
    "SpeechRecognition" in window || "webkitSpeechRecognition" in window || console.warn(
      '"@edifice.io/tiptap-extensions/speechrecognition" requires a browser supporting the SpeechRecognition API".'
    );
  },
  addCommands() {
    return {
      startSpeechRecognition: () => ({ commands }) => {
        const SpeechRecognition2 = window.SpeechRecognition || window.webkitSpeechRecognition;
        this.recognition = new SpeechRecognition2(), this.recognition.lang = this.options.lang, this.recognition.interimResults = !0, this.recognition.maxAlternatives = 1, this.recognition.continuous = !0, this.recognition.start();
        let { from, to } = this.editor.state.selection;
        return this.recognition.onresult = (event) => {
          let currentResult = "";
          for (let i = event.resultIndex; i < event.results.length; i++)
            currentResult += event.results[i][0].transcript;
          const isFinal = event.results[event.results.length - 1].isFinal;
          this.editor.commands.deleteRange({ from, to }), this.editor.commands.insertContentAt(
            from,
            isFinal ? currentResult : `<code>${currentResult}</code>`,
            { updateSelection: !isFinal }
          ), to = this.editor.state.selection.to, isFinal && (from = to);
        }, this.recognition.onerror = (event) => {
          console.log(
            `[@edifice.io/tiptap-extensions/speech-recognition][error][${event.error}]: ${event.message}`
          );
        }, this.recognition.onstart = () => {
          this.isStarted = !0;
        }, this.recognition.onend = () => {
          this.isStarted = !1;
        }, commands;
      },
      stopSpeechRecognition: () => ({ commands }) => (this.recognition.stop(), this.editor.commands.focus(), commands),
      isSpeechRecognitionStarted: () => () => this.isStarted
    };
  }
});
export {
  SpeechRecognition
};
//# sourceMappingURL=speech-recognition.js.map
