import { idiom, ng } from "entcore";

export const pdfViewer = ng.directive("pdfViewer", function () {
  return {
    restrict: "E",
    template: "",
    link: function (_scope, element, attributes) {
      if (navigator.pdfViewerEnabled) {
        let reference = window.open(attributes.ngSrc, "pdfviewer");
        reference && reference.focus();
      } else {
        let span = document.createElement("span");
        span.innerText = idiom.translate("embed.video.incompatible");
        element.append(span);
      }
    },
  };
});
