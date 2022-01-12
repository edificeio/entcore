import { ChangeDetectionStrategy, Component, Injector, OnDestroy, OnInit } from '@angular/core';
import { OdeComponent } from 'ngx-ode-core';
import { GroupsStore } from '../groups.store';


@Component({
    selector: 'ode-group-info',
    templateUrl: './group-info.component.html',
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class GroupInfoComponent extends OdeComponent implements OnInit, OnDestroy {

    // Subscribers
    groupType: string;
    
    constructor(
        injector: Injector,
        public groupsStore: GroupsStore) {
            super(injector);
    }
    
    ngOnInit(): void {
        super.ngOnInit();
    }
}
