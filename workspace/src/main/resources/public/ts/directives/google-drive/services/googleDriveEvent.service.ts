import { ng } from "entcore";
import { Observable, Subject } from "rxjs";
import { GoogleDriveDocument } from "../models/googleDriveDocument.model";

export interface IGoogleDriveEventService {
  sendDocuments(documents: {
    parentDocument: GoogleDriveDocument | null;
    documents: Array<GoogleDriveDocument>;
  }): void;
  getDocumentsState(): Observable<{
    parentDocument: GoogleDriveDocument | null;
    documents: Array<GoogleDriveDocument>;
  }>;
  sendOpenFolderDocument(document: GoogleDriveDocument): void;
  getOpenedFolderDocument(): Observable<GoogleDriveDocument>;
  getContentContext(): GoogleDriveDocument | null;
  setContentContext(content: GoogleDriveDocument | null): void;
  requestQuotaRefresh(): void;
  getQuotaRefresh(): Observable<void>;
}

const openFolderSubject = new Subject<GoogleDriveDocument>();
const documentSubject = new Subject<{
  parentDocument: GoogleDriveDocument | null;
  documents: Array<GoogleDriveDocument>;
}>();
const quotaRefreshSubject = new Subject<void>();
let contentContext: GoogleDriveDocument | null = null;

export const googleDriveEventService: IGoogleDriveEventService = {
  sendDocuments: (documents) => {
    documentSubject.next(documents);
  },

  getDocumentsState: () => {
    return documentSubject.asObservable();
  },

  sendOpenFolderDocument: (document) => {
    openFolderSubject.next(document);
  },

  getOpenedFolderDocument: () => {
    return openFolderSubject.asObservable();
  },

  getContentContext: () => {
    return contentContext;
  },

  setContentContext: (content) => {
    contentContext = content;
  },

  requestQuotaRefresh: () => {
    quotaRefreshSubject.next();
  },

  getQuotaRefresh: () => {
    return quotaRefreshSubject.asObservable();
  },
};

export const GoogleDriveEventService = ng.service(
  "GoogleDriveEventService",
  (): IGoogleDriveEventService => googleDriveEventService,
);
