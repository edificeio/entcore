import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { SpinnerService } from 'ngx-ode-ui';

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

  importFile(structureId: String, file: FileList): Promise<any>
  {
    if(file.length != 1)
      throw "There must be exactly one timetable import file";

    let form: FormData = new FormData();
    form.append("file", file[0]);

    return this.spinner.perform(
      'portal-content',
      this.httpClient.post(`/directory/timetable/import/${structureId}`, form).toPromise()
    );
  }
}
