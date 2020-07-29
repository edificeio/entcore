import {Injectable} from '@angular/core';
import {ActivatedRouteSnapshot, CanActivate} from '@angular/router';
import {pluck} from 'rxjs/operators';
import {Observable} from 'rxjs';
import {CalendarService} from './calendar.service';


@Injectable({
  providedIn: 'root'
})
export class CalendarGuardService implements CanActivate {

  constructor(private calendarService: CalendarService) {}

  public canActivate(route: ActivatedRouteSnapshot): Observable<boolean> {
    return this.calendarService.getCalendarConfKey().pipe(
        pluck('displayCalendar'));
  }
}
