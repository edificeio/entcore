import { ng, model } from "entcore";
import { CsvDelegate, CsvFile, CsvController } from "./csvViewer";
import { SyncDocument } from "./nextcloud/models/nextcloudFolder.model";
import { nextcloudService } from "./nextcloud/services/nextcloud.service";
import { TxtDelegate, TxtController, TxtFile } from "./txtViewer";
import http from "axios";

interface SyncDocumentViewerScope {
  contentType: string;
  ngModel: SyncDocument;
  isFullscreen: boolean;
  csvDelegate: CsvDelegate;
  txtDelegate: TxtDelegate;
  isTxt(): boolean;
  isStreamable(): boolean;
  download(): void;
  isOfficePdf(): boolean;
  isOfficeExcelOrCsv(): boolean;
  editImage(): void;
  fullscreen(allow: boolean): void;
  render?: () => void;
  previewUrl(): string;
  closeViewFile(): void;
  canEditInLool(): boolean;
  canEditInScratch(): boolean;
  openOnLool(): void;
  openOnScratch(): void;
  openOnGeogebra(): void;
  getFileUrl(): string;
  canEditImage(): boolean;
  hasStreamingCapability(): boolean;
  canDownload(): boolean;
  $parent: {
    display: {
      editedImage: any;
      editImage: boolean;
    };
  };
  onBack: any;
  htmlContent: string;
  $apply: any;
}

function getContentTypeFromSyncDocument(syncDoc: SyncDocument): string {
  if (!syncDoc.contentType) {
    return "unknown";
  }

  const contentType = syncDoc.contentType.toLowerCase();
  const extension = syncDoc.extension || syncDoc.name.split(".").pop()?.toLowerCase();

  // Map content types to viewer types
  if (contentType.startsWith("image/")) return "img";
  if (contentType.startsWith("video/")) return "video";
  if (contentType.startsWith("audio/")) return "audio";
  if (contentType === "application/pdf") return "pdf";
  if (contentType === "text/html") return "html";
  if (contentType === "text/csv" || extension === "csv") return "csv";
  if (contentType === "text/plain" || extension === "txt") return "txt";
  if (contentType === "application/zip") return "zip";

  // Check by extension for office documents
  if (extension) {
    switch (extension) {
      case "doc":
      case "docx":
        return "doc";
      case "ppt":
      case "pptx":
        return "ppt";
      case "xls":
      case "xlsx":
        return "xls";
      default:
        return "unknown";
    }
  }

  return "unknown";
}

function createElement(html: string): HTMLElement {
  const template = document.createElement("template");
  template.innerHTML = html.trim();
  return template.content.firstChild as HTMLElement;
}

function fadeIn(element: HTMLElement, duration: number = 400): Promise<void> {
  return new Promise((resolve) => {
    element.style.opacity = "0";
    element.style.display = "block";
    element.style.transition = `opacity ${duration}ms ease-in-out`;
    element.offsetHeight;
    element.style.opacity = "1";

    setTimeout(() => {
      element.style.transition = "";
      resolve();
    }, duration);
  });
}

function fadeOut(element: HTMLElement, duration: number = 400): Promise<void> {
  return new Promise((resolve) => {
    element.style.transition = `opacity ${duration}ms ease-in-out`;
    element.style.opacity = "0";

    setTimeout(() => {
      element.remove();
      resolve();
    }, duration);
  });
}

class SyncCsvProviderFromText implements CsvFile {
  private _cache: Promise<string>;
  constructor(private model: SyncDocument) {}
  get id() {
    return this.model.fileId?.toString() || this.model.path;
  }
  get content() {
    if (this._cache) return this._cache;
    this._cache = new Promise<string>(async (resolve, reject) => {
      try {
        const downloadUrl = nextcloudService.getFile(
          model.me.userId,
          this.model.name,
          this.model.path,
          this.model.contentType,
          false,
        );
        const response = await http.get(downloadUrl);
        resolve(response.data);
      } catch (error) {
        reject(error);
      }
    });
    return this._cache;
  }
}

class SyncCsvProviderFromExcel implements CsvFile {
  private _cache: Promise<string>;
  constructor(private model: SyncDocument) {}
  get id() {
    return this.model.fileId?.toString() || this.model.path;
  }
  get content() {
    if (this._cache) return this._cache;
    this._cache = new Promise<string>(async (resolve, reject) => {
      try {
        const downloadUrl = nextcloudService.getFile(
          model.me.userId,
          this.model.name,
          this.model.path,
          this.model.contentType,
          false,
        );
        const response = await http.get(downloadUrl);
        resolve(response.data);
      } catch (error) {
        reject(error);
      }
    });
    return this._cache;
  }
}

export const syncDocumentViewer = ng.directive("syncDocumentViewer", [
  "$sce",
  ($sce) => {
    return {
      restrict: "E",
      scope: {
        ngModel: "=",
        onBack: "&",
      },
      templateUrl: "/workspace/public/template/directives/sync-document-viewer.html",
      link: function (scope: SyncDocumentViewerScope, element, attributes) {
        const _csvCache: { [key: string]: CsvFile } = {};
        const _txtCache: { [key: string]: TxtFile } = {};
        let _csvController: CsvController = null;
        let _txtDelegate: TxtController = null;

        scope.csvDelegate = {
          onInit(ctrl) {
            _csvController = ctrl;
            _csvController.setContent(getCsvContent());
          },
        };
        scope.txtDelegate = {
          onInit(ctrl) {
            _txtDelegate = ctrl;
            _txtDelegate.setContent(getTxtContent());
          },
        };

        const getCsvContent = () => {
          const cacheKey = scope.ngModel.fileId?.toString() || scope.ngModel.path;
          if (_csvCache[cacheKey]) {
            return _csvCache[cacheKey];
          }
          if (scope.contentType == "csv") {
            _csvCache[cacheKey] = new SyncCsvProviderFromText(scope.ngModel);
          } else {
            _csvCache[cacheKey] = new SyncCsvProviderFromExcel(scope.ngModel);
          }
          return _csvCache[cacheKey];
        };

        const getTxtContent = () => {
          const cacheKey = scope.ngModel.fileId?.toString() || scope.ngModel.path;
          if (_txtCache[cacheKey]) {
            return _txtCache[cacheKey];
          }
          _txtCache[cacheKey] = new SyncCsvProviderFromText(scope.ngModel);
          return _txtCache[cacheKey];
        };

        scope.contentType = getContentTypeFromSyncDocument(scope.ngModel);

        if (scope.contentType == "html") {
          const call = async () => {
            try {
              const downloadUrl = nextcloudService.getFile(
                model.me.userId,
                scope.ngModel.name,
                scope.ngModel.path,
                scope.ngModel.contentType,
                false,
              );
              const response = await http.get(downloadUrl);
              scope.htmlContent = $sce.trustAsHtml(response.data) as string;
              scope.$apply();
            } catch (error) {
              console.error("Error loading HTML content:", error);
            }
          };
          call();
        }

        scope.isFullscreen = false;

        scope.download = function () {
          const downloadUrl = nextcloudService.getFile(
            model.me.userId,
            scope.ngModel.name,
            scope.ngModel.path,
            scope.ngModel.contentType,
            false,
          );
          window.open(downloadUrl, "_blank");
        };

        let renderElement: HTMLElement;
        let renderParent: HTMLElement;

        scope.canDownload = () => {
          return true;
        };

        scope.isOfficePdf = () => {
          const ext = ["doc", "ppt"];
          return ext.includes(scope.contentType);
        };

        scope.isOfficeExcelOrCsv = () => {
          const ext = ["xls", "csv"];
          return ext.includes(scope.contentType);
        };

        scope.isTxt = () => {
          const ext = ["txt"];
          return ext.includes(scope.contentType);
        };

        scope.isStreamable = () => {
          return scope.contentType === "video" && scope.ngModel.contentType?.startsWith("video/");
        };

        scope.closeViewFile = () => {
          scope.onBack();
        };

        scope.previewUrl = () => {
          return nextcloudService.getFile(
            model.me.userId,
            scope.ngModel.name,
            scope.ngModel.path,
            scope.ngModel.contentType,
            false,
          );
        };

        scope.getFileUrl = () => {
          return nextcloudService.getFile(
            model.me.userId,
            scope.ngModel.name,
            scope.ngModel.path,
            scope.ngModel.contentType,
            false,
          );
        };

        scope.hasStreamingCapability = () => {
          return false;
        };

        scope.fullscreen = (allow) => {
          scope.isFullscreen = allow;
          if (allow) {
            const container = createElement('<div class="fullscreen-viewer"></div>');
            container.style.display = "none";

            container.addEventListener("click", function (e) {
              const target = e.target as HTMLElement;
              if (!target.classList.contains("render")) {
                scope.fullscreen(false);
                scope.$apply("isFullscreen");
              }
            });

            const embeddedViewer = element[0].querySelector(".embedded-viewer");
            renderElement = element[0].querySelector(".render") as HTMLElement;
            renderParent = renderElement.parentElement;

            embeddedViewer?.classList.add("fullscreen");
            renderElement.classList.add("fullscreen");

            container.appendChild(renderElement);
            document.body.appendChild(container);

            fadeIn(container);

            if (typeof scope.render === "function") {
              scope.render();
            }
          } else {
            renderElement.classList.remove("fullscreen");
            renderParent.appendChild(renderElement);

            const embeddedViewer = element[0].querySelector(".embedded-viewer");
            embeddedViewer?.classList.remove("fullscreen");

            const fullscreenViewer = document.querySelector(".fullscreen-viewer") as HTMLElement;
            if (fullscreenViewer) {
              fadeOut(fullscreenViewer);
            }

            if (typeof scope.render === "function") {
              scope.render();
            }
          }
        };
      },
    };
  },
]);
