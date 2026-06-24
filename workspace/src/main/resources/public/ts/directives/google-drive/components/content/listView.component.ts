import { GoogleDriveDocument } from "../../models/googleDriveDocument.model";
import { DateUtils } from "../../utils/date.utils";

export interface IGoogleDriveViewList {
  orderByField(fieldName: string, desc?: boolean): void;
  isOrderedDesc(fieldName: string): boolean;
  isOrderedAsc(fieldName: string): boolean;
  displayLastModified(document: GoogleDriveDocument): string;

  orderField: string;
  orderDesc: boolean;
}

export class GoogleDriveViewList implements IGoogleDriveViewList {
  private vm: any;

  orderField: string;
  orderDesc: boolean;

  constructor(vm: any) {
    this.vm = vm;
    this.orderField = null;
    this.orderDesc = false;
  }

  isOrderedAsc(fieldName: string): boolean {
    return this.orderField === fieldName && !this.orderDesc;
  }

  isOrderedDesc(fieldName: string): boolean {
    return this.orderField === fieldName && this.orderDesc;
  }

  orderByField(fieldName: string, desc?: boolean): void {
    if (fieldName === this.orderField) {
      this.orderDesc = !this.orderDesc;
    } else {
      this.orderDesc = typeof desc === "boolean" ? desc : false;
    }
    this.orderField = fieldName;
    this.vm.documents = this.vm.documents.sort(
      (a: GoogleDriveDocument, b: GoogleDriveDocument) => {
        if (this.orderField === "modifiedTime") {
          return this.orderDesc
            ? new Date(a.modifiedTime).getTime() - new Date(b.modifiedTime).getTime()
            : new Date(b.modifiedTime).getTime() - new Date(a.modifiedTime).getTime();
        } else if (this.orderField === "size") {
          if (a.size > b.size) return this.orderDesc ? 1 : -1;
          if (a.size < b.size) return this.orderDesc ? -1 : 1;
        } else if (typeof a[fieldName] === "string") {
          return this.orderDesc
            ? a[fieldName].localeCompare(b[fieldName])
            : b[fieldName].localeCompare(a[fieldName]);
        }
        return 0;
      },
    );
  }

  displayLastModified(document: GoogleDriveDocument): string {
    if (!document.modifiedTime) return "";
    return DateUtils.format(document.modifiedTime, "DD/MM/YYYY HH:mm:ss");
  }
}
