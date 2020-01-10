import { ChangeDetectionStrategy, Component, Injector, OnDestroy, OnInit } from '@angular/core';
import { OdeComponent } from 'ngx-ode-core';

@Component({
    selector: 'ode-users-root',
    templateUrl: './users.component.html',
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class UsersComponent extends OdeComponent implements OnInit, OnDestroy {

    // Tabs
    tabs = [
        {label: 'users.tabs.mainList', view: 'list'},
        {label: 'users.tabs.removedList', view: 'relink'},
    ];

    constructor(
        injector: Injector
    ) {
            super(injector);
    }
}

export function isEqual(arr1: Array<any>, arr2: Array<any>): boolean {
    if (arr1.length !== arr2.length) {
        return false;
    } else {
        for (let i = 0; i < arr1.length; i++) {
            if (arr1[i] !== arr2[i]) {
                return false;
            }
        }
        return true;
    }
}

export function includes(arr1: Array<any>, arr2: Array<any>) {
    let res = false;
    arr1.forEach(a => {
        if (isEqual(a, arr2)) {
            res = true;
        }
    });
    return res;
}
