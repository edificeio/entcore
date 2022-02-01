import { ChangeDetectionStrategy, Component, Injector, OnDestroy, OnInit } from '@angular/core';
import { Session } from 'src/app/core/store/mappings/session';
import { SessionModel } from 'src/app/core/store/models/session.model';
import { OdeComponent } from 'ngx-ode-core';
import { routing } from '../core/services/routing.service';
import { Data } from '@angular/router';
import { StructureModel } from '../core/store/models/structure.model';

@Component({
    selector: 'ode-users-root',
    templateUrl: './users.component.html',
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class UsersComponent extends OdeComponent implements OnInit, OnDestroy {

    // Tabs
    tabs = [
        {label: 'users.tabs.mainList', view: 'list', active: 'list'},
        {label: 'users.tabs.treeList', view: 'tree-list', active: 'tree-list'}
    ];

    constructor(
        injector: Injector
    ) {
        super(injector);
        this.admcSpecific();
    }

    structure: StructureModel;

    async ngOnInit() {
        this.subscriptions.add(routing.observe(this.route, 'data').subscribe((data: Data) => {
            if (data.structure) {
                this.structure = data.structure;
                this.changeDetector.markForCheck();
            }
        }));
    }

    async admcSpecific() {
        const session: Session = await SessionModel.getSession();
        if(session.isADMC() == true)
        {
            this.tabs.splice(1, 0, {label: 'users.tabs.removedList', view: 'relink', active: 'relink'});
        }
    }

    isActive(path: string): boolean {
        return this.router.isActive('/admin/' + this.structure.id + '/users/' + path, false);
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
