import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { SpinnerService } from 'ngx-ode-ui';
import { TimetableClassesMapping, TimetableGroupsMapping, EDTImportFlux } from './import-edt.component';

@Injectable({
  providedIn: 'root',
})
export class ImportTimetableService
{
  constructor(private httpClient: HttpClient, private spinner: SpinnerService) {}
  
  setFluxType(structureId: String, type: String): Observable<any>
  {
    return this.httpClient.put(`/directory/timetable/init/${structureId}`, {
      type: type,
    });
  }

  importFile(structureId: String, importType: EDTImportFlux, file: FileList): Promise<any>
  {
    if(file.length != 1)
      throw "There must be exactly one timetable import file";

    let form: FormData = new FormData();
    form.append("file", file[0]);

    let type = importType != null ? `${importType}/` : "";

    return this.spinner.perform(
      'portal-content',
      this.httpClient.post(`/directory/timetable/import/${type}${structureId}`, form).toPromise()
    );
  }

  getClassesMapping(structureId: String): Observable<TimetableClassesMapping>
  {
    return this.httpClient.get<TimetableClassesMapping>(`/directory/timetable/classes/${structureId}`);
  }

  updateClassesMapping(structureId: String, cm: TimetableClassesMapping): Observable<any>
  {
    return this.httpClient.put(`/directory/timetable/classes/${structureId}`, cm);
  }

  getGroupsMapping(structureId: String): Observable<TimetableGroupsMapping>
  {
    return this.httpClient.get<TimetableGroupsMapping>(`/directory/timetable/groups/${structureId}`);
  }

  updateGroupsMapping(structureId: String, gm: TimetableGroupsMapping): Observable<any>
  {
    return this.httpClient.put(`/directory/timetable/groups/${structureId}`, gm);
  }
}
