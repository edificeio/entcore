import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { SpinnerService } from 'ngx-ode-ui';

@Injectable({
  providedIn: 'root',
})
export class StructureInformationsService
{
  constructor(private httpClient: HttpClient, private spinner: SpinnerService) {}

  update(structureId: string, newName: string, newUAI: string, newHasApp: boolean): Observable<any>
  {
    let params = {
      name: newName,
      UAI: newUAI,
      hasApp: newHasApp == true ? true : false,
    };
    return this.httpClient.put(`/directory/structure/${structureId}`, params);
  }

  resetManualName(structureId: string): Observable<any>
  {
    return this.httpClient.put(`/directory/structure/${structureId}/resetName`, null);
  }

  detachParent(structureId: string, parentId: string): Observable<any>
  {
    return this.httpClient.delete(`/directory/structure/${structureId}/parent/${parentId}`)
  }

  getMetrics(structureId: string): Observable<any>
  {
    return this.httpClient.get(`/directory/structure/${structureId}/metrics`);
  }
}
