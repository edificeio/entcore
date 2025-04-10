import { Node } from "@tiptap/core";
class SS_Node extends Node {
  static create(config) {
    return Node.create(config);
  }
}
const SpeechSynthesis = SS_Node.create({
  name: "speechSynthesis",
  addOptions() {
    return {
      lang: "fr-FR",
      pitch: 1
    };
  },
  addCommands() {
    return {
      startSpeechSynthesis: () => ({ commands }) => (this.speechSynthesis = new SpeechSynthesisUtterance(), this.speechSynthesis.lang = this.options.lang, this.speechSynthesis.pitch = this.options.pitch, this.speechSynthesis.text = this.editor.getText(), window.speechSynthesis.speak(this.speechSynthesis), commands),
      stopSpeechSynthesis: () => ({ commands }) => (window.speechSynthesis.cancel(), commands)
    };
  }
});
export {
  SpeechSynthesis
};
//# sourceMappingURL=speech-synthesis.js.map
