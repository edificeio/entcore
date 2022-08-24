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
        {label: 'users.tabs.mainList', view: 'list', active: 'list'}
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

                // if structure has children then add "User Transverse search" tab
                const treeListTabPosition = this.tabs.findIndex(tab => tab.label === 'users.tabs.treeList');
                if (this.structure.children && 
                    this.structure.children.length > 0 &&
                    treeListTabPosition === -1) {
                    this.tabs.push({label: 'users.tabs.treeList', view: 'tree-list', active: 'tree-list'});
                } else if (!this.structure.children && treeListTabPosition > -1) {
                    this.tabs.splice(treeListTabPosition, 1);
                    if (this.isActive('tree-list')) {
                        this.router.navigate(['/admin', this.structure.id, 'users', 'list', 'filter']);
                    }
                }
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

