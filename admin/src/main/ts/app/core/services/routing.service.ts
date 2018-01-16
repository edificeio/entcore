import { ActivatedRoute, ActivatedRouteSnapshot, Data, Params } from '@angular/router'
import { Observable } from 'rxjs/Observable'
import 'rxjs/add/observable/merge'

export let routing = {
    get: function(item: String, field: "params" | "data" , route: ActivatedRouteSnapshot) : any {
        if(route[field][<any> item]) {
            return route[field][<any> item]
        }

        return route.parent ? this.get(item, field, route.parent) : undefined
    },
    getData: function(route: ActivatedRouteSnapshot, item: String) : any {
        return this.get(item, "data", route)
    },
    getParam: function(route: ActivatedRouteSnapshot, item: String) : String {
        return this.get(item, "params", route)
    },
    observe: function(route: ActivatedRoute, to: "params" | "data") : Observable<Params> | Observable<Data> {
        let observables:  (Observable<Params> | Observable<Data>)[] = route.pathFromRoot.map(route => route[to])

        return Observable.merge(...observables)
    }
}