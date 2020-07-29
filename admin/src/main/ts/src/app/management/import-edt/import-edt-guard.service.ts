import {Injectable} from '@angular/core';
import {ActivatedRouteSnapshot, CanActivate} from '@angular/router';
import {pluck} from 'rxjs/operators';
import {Observable} from 'rxjs';
import {ImportEDTReportsService} from './import-edt-reports.service';


@Injectable({
  providedIn: 'root'
})
export class ImportEdtGuardService implements CanActivate {

  constructor(private importEDTReportsService: ImportEDTReportsService) {}

  public canActivate(route: ActivatedRouteSnapshot): Observable<boolean> {
    return this.importEDTReportsService.getEdtConfKey().pipe(
        pluck('displayEdt'));
  }
}
