import { SyncDocument } from "../../models/nextcloudFolder.model";
import { DateUtils } from "../../utils/date.utils";

export interface INextcloudViewList {
  orderByField(fieldName: string, desc?: boolean): void;
  isOrderedDesc(fieldName: string): boolean;
  isOrderedAsc(fieldName: string): boolean;
  displayLastModified(document: SyncDocument): string;

  orderField: string;
  orderDesc: boolean;
}

export class NextcloudViewList implements INextcloudViewList {
  private vm: any;

  orderField: string;
  orderDesc: boolean;

  constructor(vm) {
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
      (a: SyncDocument, b: SyncDocument) => {
        if (this.orderField === "lastModified") {
          return this.orderDesc
            ? new Date(a.lastModified).getTime() -
                new Date(b.lastModified).getTime()
            : new Date(b.lastModified).getTime() -
                new Date(a.lastModified).getTime();
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

  displayLastModified(document: SyncDocument): string {
    if (!document.lastModified) return "";
    return DateUtils.format(document.lastModified, "DD/MM/YYYY HH:mm:ss");
  }
}
