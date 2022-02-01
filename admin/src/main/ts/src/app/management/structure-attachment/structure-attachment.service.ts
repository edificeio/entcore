import { HttpClient } from "@angular/common/http";
import { Injectable } from "@angular/core";
import { Observable } from "rxjs";
import { SpinnerService } from "ngx-ode-ui";

@Injectable({
  providedIn: "root",
})
export class StructureAttachmentService {
  constructor(
    private httpClient: HttpClient,
    private spinner: SpinnerService
  ) {}

  defineParent = (structureId: string, parentStructureId: string): Observable<any> => {
    let params = {
      structureId,
      parentStructureId,
    };
    return this.httpClient.put(
      `/directory/structure/${structureId}/parent/${parentStructureId}`,
      { params }
    );
  };

  detachParent = (structureId: string, parentStructureId: string): Observable<any> => {
    return this.httpClient.delete(
      `/directory/structure/${structureId}/parent/${parentStructureId}`
    );
  };
}
