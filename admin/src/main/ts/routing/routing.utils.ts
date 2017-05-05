import { ActivatedRoute, ActivatedRouteSnapshot } from '@angular/router'
import { Observable } from "rxjs"

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
    observe: function(route: ActivatedRoute, to: "params" | "data") : Observable<{[key: string]: any}> {
        let observables: Observable<{[key: string]: any}>[] = route.pathFromRoot.map(route => route[to])

        return Observable.merge(...observables)
    }
}