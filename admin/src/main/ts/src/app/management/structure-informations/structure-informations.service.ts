import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { SpinnerService } from 'ngx-ode-ui';
import { StructureModel } from 'src/app/core/store/models/structure.model';
import { DuplicationSettings } from './structure-informations.component';

@Injectable({
  providedIn: 'root',
})
export class StructureInformationsService
{
  constructor(private httpClient: HttpClient, private spinner: SpinnerService) {}

  update(structureId: string, newName: string, newUAI: string, newHasApp: boolean, newIgnoreMFA: boolean): Observable<any>
  {
    let params = {
      name: newName,
      UAI: newUAI ? newUAI : "",
      hasApp: newHasApp == true ? true : false,
      ignoreMFA : newIgnoreMFA
    };
    return this.httpClient.put(`/directory/structure/${structureId}`, params);
  }

  resetManualName(structureId: string): Observable<any>
  {
    return this.httpClient.put(`/directory/structure/${structureId}/resetName`, null);
  }

  getMetrics(structureId: string): Observable<any>
  {
    return this.httpClient.get(`/directory/structure/${structureId}/metrics`);
  }

  checkUAIs(uaiArray: string[]): Observable<StructureModel[]>
  {
    return this.httpClient.put(`/directory/structure/check/uai`, {list: uaiArray}) as Observable<StructureModel[]>;
  }
  duplicate(structure: StructureModel, options: DuplicationSettings): Observable<StructureModel[]>
  {
    let params = {
      list: options.structures.map(struc => struc.UAI),
      options: {
        setApplications: options.applications,
        setWidgets: options.widgets,
        setDistribution: options.distribution,
        setEducation: options.education,
        setHasApp: options.mobileapp
      },
      infos: {
        structure: structure.name,
        targets: options.structures.map(struc => { return { name: `${struc.UAI} - ${struc.name}`}; })
      }
    }
    return this.httpClient.put(`/directory/structure/${structure.id}/duplicate`, params) as Observable<any>;
  }
}
