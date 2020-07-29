import {Injectable} from '@angular/core';
import {ActivatedRouteSnapshot, CanActivateChild} from '@angular/router';
import {pluck} from 'rxjs/operators';
import {Observable} from 'rxjs';
import {SubjectsService} from './subjects.service';


@Injectable({
  providedIn: 'root'
})
export class SubjectsGuardService implements CanActivateChild {

  constructor(private subjectsService: SubjectsService) {}

  public canActivateChild(route: ActivatedRouteSnapshot): Observable<boolean> {
    return this.subjectsService.getSubjectsConfKey().pipe(
        pluck('displaySubjects'));
  }
}

