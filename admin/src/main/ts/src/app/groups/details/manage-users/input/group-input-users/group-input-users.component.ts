import { OdeComponent } from './../../../../../core/ode/OdeComponent';
import { ChangeDetectionStrategy, Component, Input, OnDestroy, OnInit, Injector } from '@angular/core';

import {GroupsStore} from '../../../../groups.store';
import {SelectOption} from 'ngx-ode-ui';
import {OrderPipe} from 'ngx-ode-ui';
import {DeleteFilter, UserlistFiltersService} from '../../../../../core/services/userlist.filters.service';
import { UserListService } from 'src/app/core/services/userlist.service';
import { UserModel } from 'src/app/core/store/models/user.model';
import { StructureModel } from 'src/app/core/store/models/structure.model';
import { SpinnerService } from 'ngx-ode-ui';
import { NotifyService } from 'src/app/core/services/notify.service';
import { globalStore } from 'src/app/core/store/global.store';

@Component({
    selector: 'ode-group-input-users',
    templateUrl: './group-input-users.component.html',
    providers: [UserListService],
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class GroupInputUsersComponent extends OdeComponent implements OnInit, OnDestroy {
    @Input() model: UserModel[] = [];

    public excludeDeletedUsers: DeleteFilter;

    // list elements stored by store pipe in list component
    // (takes filters in consideration)
    storedElements: UserModel[] = [];

    // Users selected by enduser
    selectedUsers: UserModel[] = [];

    structure: StructureModel;
    structures: StructureModel[] = [];

    structureOptions: SelectOption<StructureModel>[] = [];

    constructor(private groupsStore: GroupsStore,
                public userLS: UserListService,
                private spinner: SpinnerService,
                private ns: NotifyService,
                injector: Injector,
                private orderPipe: OrderPipe,
                public listFilters: UserlistFiltersService) {
                    super(injector);
        this.excludeDeletedUsers = new DeleteFilter(listFilters.$updateSubject);
        this.excludeDeletedUsers.outputModel = ["users.not.deleted"];
    }

    ngOnInit(): void {
        super.ngOnInit();
        this.structure = this.groupsStore.structure;
        this.structures = globalStore.structures.data;
        this.structureOptions = this.orderPipe.transform(this.structures, '+name')
            .map(structure => ({value: structure, label: structure.name}));

        this.subscriptions.add(this.listFilters.$updateSubject.subscribe(() => {
            this.changeDetector.markForCheck();
        }));
    }

   
    selectUser(u: UserModel): void {
        if (this.selectedUsers.indexOf(u) === -1) {
            this.selectedUsers.push(u);
        } else {
            this.selectedUsers = this.selectedUsers.filter(su => su.id !== u.id);
        }
    }

    isSelected = (user: UserModel) => {
        return this.selectedUsers.indexOf(user) > -1;
    }

    selectAll(): void {
        this.selectedUsers = this.storedElements;
    }

    deselectAll(): void {
        this.selectedUsers = [];
    }

    structureChange(s: StructureModel): void {
        const selectedStructure: StructureModel = globalStore.structures.data.find(
            globalS => globalS.id === s.id);
        this.structure = selectedStructure;

        if (selectedStructure.users && selectedStructure.users.data
            && selectedStructure.users.data.length < 1) {
            this.spinner.perform('group-manage-users',
                selectedStructure.users.sync()
                    .then(() => {
                        this.model = selectedStructure.users.data
                            .filter(u =>
                                this.groupsStore.group.users.map(x => x.id).indexOf(u.id) === -1);
                        this.changeDetector.markForCheck();
                    })
                    .catch((err) => {
                        this.ns.error(
                            {
                                key: 'notify.structure.syncusers.error.content'
                                , parameters: {structure: s.name}
                            }
                            , 'notify.structure.syncusers.error.title'
                            , err);
                    })
            );
        } else {
            this.model = selectedStructure.users.data
                .filter(u => this.groupsStore.group.users.map(x => x.id).indexOf(u.id) === -1);
            this.changeDetector.markForCheck();
        }
    }

    addUsers(): void {
        this.spinner.perform('group-manage-users',
            this.groupsStore.group.addUsers(this.selectedUsers)
                .then(() => {
                    this.groupsStore.group.users = this.groupsStore.group.users.concat(this.selectedUsers);
                    this.model = this.model.filter(u => this.selectedUsers.indexOf(u) === -1);
                    this.selectedUsers = [];
                    this.ns.success('notify.group.manage.users.added.content');
                    this.changeDetector.markForCheck();
                })
                .catch((err) => {
                    this.ns.error('notify.group.manage.users.added.error.content'
                        , 'notify.group.manage.users.added.error.title', err);
                })
        );
    }
}
