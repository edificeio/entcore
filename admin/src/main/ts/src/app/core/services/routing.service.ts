import {ActivatedRoute, ActivatedRouteSnapshot, Data, Params} from '@angular/router';
import {merge, Observable} from 'rxjs';

export let routing = {
    get(item: string, field: 'params' | 'data', route: ActivatedRouteSnapshot): any {
        if (route[field][item as any]) {
            return route[field][item as any];
        }

        return route.parent ? this.get(item, field, route.parent) : undefined;
    },
    getData(route: ActivatedRouteSnapshot, item: string): any {
        return this.get(item, 'data', route);
    },
    getParam(route: ActivatedRouteSnapshot, item: string): string {
        return this.get(item, 'params', route);
    },
    observe(route: ActivatedRoute, to: 'params' | 'data'): Observable<Params> | Observable<Data> {
        const observables: (Observable<Params> | Observable<Data>)[] = route.pathFromRoot.map(rRoute => rRoute[to]);

        return merge(...observables);
    }
};
