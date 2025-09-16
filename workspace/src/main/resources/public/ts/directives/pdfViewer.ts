import { $, idiom, ng } from "entcore";

export const pdfViewer = ng.directive("pdfViewer", function () {
  return {
    restrict: "E",
    template: "",
    link: function (_scope, element, attributes) {
      if (navigator.pdfViewerEnabled) {
        var frame = document.createElement("iframe");
        $(frame).addClass("render");
        frame.src = attributes.ngSrc + "?inline=true";
        frame.style.width = "100%";
        frame.style.height = "100vh";
        element.append(frame);
      } else {
        let span = document.createElement("span");
        span.innerText = idiom.translate("embed.video.incompatible");
        element.append(span);
      }
    },
  };
});
