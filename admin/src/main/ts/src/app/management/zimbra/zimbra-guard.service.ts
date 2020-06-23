import {Injectable} from '@angular/core';
import {ActivatedRouteSnapshot, CanActivate} from '@angular/router';
import {ZimbraService} from './zimbra.service';
import {pluck} from 'rxjs/operators';
import {Observable} from 'rxjs';


@Injectable({
  providedIn: 'root'
})
export class ZimbraGuardService implements CanActivate {

  constructor(private zimbraService: ZimbraService) {}

  public canActivate(route: ActivatedRouteSnapshot): Observable<boolean> {
    return this.zimbraService.getZimbraConfKey().pipe(
        pluck('displayZimbra'));
  }
}
