import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { SpinnerService } from 'ngx-ode-ui';
import { StructureModel } from 'src/app/core/store/models/structure.model';

@Injectable({
  providedIn: 'root',
})
export class GarService
{
  constructor(private httpClient: HttpClient, private spinner: SpinnerService) {}

  getGarConfig(): Observable<any>
  {
      return this.httpClient.get(`/directory/gar/config`);
  }

  checkGAR(uaiArray: string[]): Observable<StructureModel[]>
  {
    return this.httpClient.put(`/directory/structure/check/gar`, {uais: uaiArray}) as Observable<StructureModel[]>;
  }

  applyGAR(uaiArray: string[], garId: string): Observable<any>
  {
    return this.httpClient.put(`/directory/structure/gar/activate`, {uais: uaiArray, garId: garId});
  }
}
