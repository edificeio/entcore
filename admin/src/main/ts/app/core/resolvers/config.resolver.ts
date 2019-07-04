import { Injectable } from '@angular/core';
import { ActivatedRouteSnapshot, Resolve } from '@angular/router';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable } from 'rxjs/Observable';

import { Config } from './Config';

@Injectable()
export class ConfigResolver implements Resolve<Config> {
    constructor(private http: HttpClient) {
    }

    resolve(route: ActivatedRouteSnapshot): Observable<Config> {
        return this.http.get<Config>('/admin/api/platform/config');
    }
}