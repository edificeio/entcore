import http, { AxiosError } from "axios";
import { model, notify } from "entcore";
import { SyncDocument } from "../../models/nextcloudFolder.model";
import { nextcloudService } from "../../services/nextcloud.service";
import { nextcloudUserService } from "../../services/nextcloudUser.service";
import { safeApply } from "../../utils/safeApply.utils";
import { IWorkspaceNextcloudContent } from "./contentViewer.component";

declare let window: any;
declare var ENABLE_LOOL: boolean;

export interface INextcloudCreateDocumentType {
  extension: string;
  label: string;
  description: string;
  icon: string;
  contentType: string;
}

// Blank templates and icons are served by the lool app, whose create button this mirrors (visually too
// — see the card layout in workspace-nextcloud-create-document.html, matching lool's own modal).
const NEXTCLOUD_CREATE_DOCUMENT_TYPES: Array<INextcloudCreateDocumentType> = [
  {
    extension: "docx",
    label: "nextcloud.create.document.type.docx",
    description: "nextcloud.create.document.type.docx.description",
    icon: "/lool/public/img/text.svg",
    contentType: "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
  },
  {
    extension: "xlsx",
    label: "nextcloud.create.document.type.xlsx",
    description: "nextcloud.create.document.type.xlsx.description",
    icon: "/lool/public/img/sheet.svg",
    contentType: "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
  },
  {
    extension: "pptx",
    label: "nextcloud.create.document.type.pptx",
    description: "nextcloud.create.document.type.pptx.description",
    icon: "/lool/public/img/presentation.svg",
    contentType: "application/vnd.openxmlformats-officedocument.presentationml.presentation",
  },
];
const LOOL_TEMPLATE_URL: string = "/lool/public/lool-templates/template.";
const FORBIDDEN_FILENAME_CHARS: RegExp = /[/\\<>|:*?"]/g;

interface IViewModel {
  lightbox: boolean;
  docTypes: Array<INextcloudCreateDocumentType>;
  selectedType: INextcloudCreateDocumentType;
  name: string;

  isEnabled(): boolean;
  toggleCreateDocumentView(state: boolean): void;
  selectType(docType: INextcloudCreateDocumentType): void;
  canCreate(): boolean;
  createDocument(): Promise<void>;
}

export class CreateDocumentSnipletViewModel implements IViewModel {
  private vm: IWorkspaceNextcloudContent;

  lightbox: boolean;
  docTypes: Array<INextcloudCreateDocumentType>;
  selectedType: INextcloudCreateDocumentType;
  name: string;

  constructor(scope: IWorkspaceNextcloudContent) {
    this.vm = scope;
    this.lightbox = false;
    this.docTypes = NEXTCLOUD_CREATE_DOCUMENT_TYPES;
    this.selectedType = this.docTypes[0];
    this.name = "";
    this.loadAvailableTypes();
  }

  // Same gate as the lool create button of the personal space (the workflow is checked in the template).
  isEnabled(): boolean {
    return typeof ENABLE_LOOL !== "undefined" && !!ENABLE_LOOL;
  }

  toggleCreateDocumentView(state: boolean): void {
    this.lightbox = state;
    if (state) {
      this.selectedType = this.docTypes[0];
      this.name = "";
    }
    safeApply(this.vm);
  }

  selectType(docType: INextcloudCreateDocumentType): void {
    this.selectedType = docType;
  }

  canCreate(): boolean {
    return !!this.selectedType && !!this.sanitizeName(this.name);
  }

  async createDocument(): Promise<void> {
    if (!this.canCreate() || this.vm.isImporting) return;
    const docType: INextcloudCreateDocumentType = this.selectedType;
    const fileName: string = this.buildFileName(this.name, docType);
    const folder: SyncDocument =
      this.vm.getNextcloudTreeController()["selectedFolder"] || this.vm.parentDocument;
    const folderPath: string = folder && folder.path ? folder.path : null;
    // Opened during the click, otherwise popup blockers reject it once the upload has resolved.
    const editorWindow: any = window.open("", "_blank");

    this.toggleCreateDocumentView(false);
    this.vm.isImporting = true;
    safeApply(this.vm);
    try {
      const existingPaths: Set<string> = new Set(
        (await nextcloudService.listDocument(model.me.userId, folderPath)).map((doc: SyncDocument) => doc.path),
      );
      const templateResponse = await http.get(LOOL_TEMPLATE_URL + docType.extension, { responseType: "blob" });
      const file: File = new File([templateResponse.data], fileName, { type: docType.contentType });

      const uploadResponse = await nextcloudService.uploadDocuments(model.me.userId, [file], folderPath);
      // Per-file failures come back inside a 200 response.
      const results: Array<any> = Array.isArray(uploadResponse.data) ? uploadResponse.data : [];
      if (results.some((result: any) => result && result.error)) {
        throw new Error("Nextcloud upload failed: " + JSON.stringify(results));
      }

      const documents: Array<SyncDocument> = await nextcloudService.listDocument(model.me.userId, folderPath);
      this.vm.documents = documents
        .filter((doc: SyncDocument) => doc.path != folderPath)
        .filter((doc: SyncDocument) => doc.name != model.me.userId);
      this.vm.getNextcloudTreeController().userInfo = await nextcloudUserService.getUserInfo(model.me.userId);

      // The upload response has neither the fileId nor the final name (renamed on duplicates).
      const created: SyncDocument = this.findCreatedDocument(documents, existingPaths, fileName);
      if (!created) {
        throw new Error("Created document not found in folder " + folderPath);
      }
      const editUrl: string = `${this.vm.nextcloudUrl}/index.php/apps/onlyoffice/${created.fileId}`;
      if (editorWindow) {
        editorWindow.location.href = editUrl;
      } else {
        window.open(editUrl, "_blank");
      }
    } catch (err) {
      if (editorWindow) editorWindow.close();
      console.error("Error while creating nextcloud document: " + ((err as AxiosError).message || err));
      notify.error("nextcloud.create.document.error");
    } finally {
      this.vm.isImporting = false;
      safeApply(this.vm);
    }
  }

  private findCreatedDocument(
    documents: Array<SyncDocument>,
    existingPaths: Set<string>,
    fileName: string,
  ): SyncDocument {
    const newFiles: Array<SyncDocument> = documents.filter(
      (doc: SyncDocument) => !doc.isFolder && !existingPaths.has(doc.path),
    );
    const baseName: string = fileName.substring(0, fileName.lastIndexOf("."));
    return newFiles.find((doc: SyncDocument) => doc.name && doc.name.startsWith(baseName)) || newFiles[0];
  }

  private loadAvailableTypes(): void {
    if (!this.isEnabled()) return;
    http
      .get("/lool/providers/context")
      .then((res) => {
        const templates: Array<string> = res.data && res.data.templates;
        if (!Array.isArray(templates)) return;
        const available = NEXTCLOUD_CREATE_DOCUMENT_TYPES.filter((docType) => templates.includes(docType.extension));
        if (available.length) {
          this.docTypes = available;
          this.selectedType = available[0];
          safeApply(this.vm);
        }
      })
      .catch((err: AxiosError) => console.error("Error while fetching lool provider context: " + err.message));
  }

  private sanitizeName(name: string): string {
    return (name || "").replace(FORBIDDEN_FILENAME_CHARS, "").trim();
  }

  private buildFileName(name: string, docType: INextcloudCreateDocumentType): string {
    const cleanName: string = this.sanitizeName(name);
    const extension: string = "." + docType.extension;
    return cleanName.toLowerCase().endsWith(extension) ? cleanName : cleanName + extension;
  }
}
