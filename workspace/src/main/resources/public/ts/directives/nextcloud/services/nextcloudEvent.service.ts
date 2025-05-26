import { ng } from "entcore";
import { Observable, Subject } from "rxjs";
import { SyncDocument } from "../models/nextcloudFolder.model";

export interface INextcloudEventService {
  sendDocuments(documents: {
    parentDocument: SyncDocument;
    documents: Array<SyncDocument>;
  }): void;
  getDocumentsState(): Observable<{
    parentDocument: SyncDocument;
    documents: Array<SyncDocument>;
  }>;
  sendOpenFolderDocument(document: SyncDocument): void;
  getOpenedFolderDocument(): Observable<SyncDocument>;
  getContentContext(): SyncDocument;
  setContentContext(content: SyncDocument): void;
}

const openFolderSubject = new Subject<SyncDocument>();
const documentSubject = new Subject<{
  parentDocument: SyncDocument;
  documents: Array<SyncDocument>;
}>();
let contentContext: SyncDocument = null;

export const nextcloudEventService: INextcloudEventService = {
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
};

export const NextcloudEventService = ng.service(
  "NextcloudEventService",
  (): INextcloudEventService => nextcloudEventService,
);
