import { Injectable } from '@angular/core';
import { ActivatedRouteSnapshot, Resolve } from '@angular/router';
import { Config } from "./Config";
import { Http } from "@angular/http";
import { Observable } from "rxjs";
import "rxjs/add/operator/map";

@Injectable()
export class ConfigResolver implements Resolve<Config | Error> {
    constructor(private http: Http) {
    }

    resolve(route: ActivatedRouteSnapshot): Observable<Config> {
        return this.http.get('/admin/api/platform/config')
            .map(response => response.json());
    }
}