import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import { EDTReport } from './import-edt.component';
import {BundlesService} from 'ngx-ode-sijil';

@Injectable({
  providedIn: 'root',
})
export class ImportEDTReportsService {
  constructor(private httpClient: HttpClient, private bundlesServices: BundlesService) {}

  getList(structureId: string): Observable<EDTReport[]> {
    return this._format(this.httpClient.get<EDTReport[]>(`/directory/timetable/import/${structureId}/reports`));
  }

  getReport(structureId: string, reportId: string): Observable<EDTReport> {
    return this.httpClient.get<EDTReport>(`/directory/timetable/import/${structureId}/report/${reportId}`);
  }

  /**
   * Get the value of the configuration key "displayEdt"
   */
  public getEdtConfKey(): Observable<{ displayEdt: boolean }> {
    return this.httpClient.get<{ displayEdt: boolean }>(`/admin/api/config/timetable/import`);
  }

  private _format(request: Observable<EDTReport[]>): Observable<EDTReport[]> {
    return request.pipe(
      map(data => {
        for (let i = data.length; i-- > 0;) {
          data[i].id = data[i]._id;
          data[i].date = new Date(data[i].created.$date).toLocaleString(this.bundlesServices.currentLanguage);
        }

        return data;
      })
    );
  }
}
