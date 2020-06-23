import {Injectable} from '@angular/core';
import {ActivatedRouteSnapshot, CanActivate} from '@angular/router';
import {ZimbraService} from './zimbra.service';

@Injectable({
  providedIn: 'root'
})
export class ZimbraGuardService implements CanActivate {

  public displayZimbra: boolean;

  constructor(private zimbraService: ZimbraService) {}

  public canActivate(route: ActivatedRouteSnapshot) {

    this.zimbraService.getZimbraConfKey().subscribe((data) => {
      this.displayZimbra = data.displayZimbra;
    });
    return this.displayZimbra;
  }
}
