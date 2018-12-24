import { Component, Input, Output, ChangeDetectionStrategy, ChangeDetectorRef
    , OnInit, EventEmitter } from '@angular/core'
import { ActivatedRoute } from '@angular/router'
import { Subscription } from 'rxjs/Subscription'

import { UserListService, UserlistFiltersService, SpinnerService
    , NotifyService } from '../../../../core/services'
import { GroupsStore } from '../../../groups.store'
import { UserModel, StructureModel, globalStore } from '../../../../core/store'

@Component({
    selector: 'group-input-users',
    template: `
        <div class="filters">
            <select [ngModel]="structure" (ngModelChange)="structureChange($event)" name="structure">
                <option *ngFor="let s of structures | orderBy: ['+name']" [ngValue]="s">{{ s.name }}</option>
            </select>
            <group-input-filters-users [structure]="structure">
            </group-input-filters-users>
        </div>  

        <div class="flex-row-wrap">
            <list
                [model]="model"
                [filters]="listFilters.getFormattedFilters()"
                [inputFilter]="userLS.filterByInput"
                [sort]="userLS.sorts"
                [isSelected]="isSelected"
                (inputChange)="userLS.inputFilter = $event"
                (onSelect)="selectUser($event)"
                (listChange)="storedElements = $event"
                noResultsLabel="list.results.no.users">
                <div toolbar class="user-toolbar">
                    <i class="fa" aria-hidden="true"
                        [ngClass]="{
                            'fa-sort-alpha-asc': userLS.sortsMap.alphabetical.sort === '+',
                            'fa-sort-alpha-desc': userLS.sortsMap.alphabetical.sort === '-',
                            'selected': userLS.sortsMap.alphabetical.selected
                        }"
                        [title]="'sort.alphabetical' | translate" position="top"
                        (click)="userLS.changeSorts('alphabetical')"></i>

                    <i class="fa" aria-hidden="true"
                        [ngClass]="{
                            'fa-sort-amount-asc': userLS.sortsMap.profile.sort === '+',
                            'fa-sort-amount-desc': userLS.sortsMap.profile.sort === '-',
                            'selected': userLS.sortsMap.profile.selected
                        }"
                        [title]="'sort.profile' | translate" position="top"
                        (click)="userLS.changeSorts('profile')"></i>

                    <button class="select-all" (click)="selectAll()" 
                        [title]="'select.all' | translate">
                        <s5l>select.all</s5l>
                    </button>

                    <button class="deselect-all" (click)="deselectAll()"
                        [title]="'deselect.all' | translate">
                        <s5l>deselect.all</s5l>
                    </button>
                </div>
                
                <ng-template let-item>
                    <span class="display-name">
                        {{item?.lastName.toUpperCase()}} {{item?.firstName}}
                    </span>
                    <i class="profile" [ngClass]="item.type">{{item.type | translate}}</i>
                </ng-template>
            </list>

            <button (click)="addUsers()"
                [disabled]="selectedUsers.length === 0"
                class="add"
                [title]="'group.manage.users.button.add' | translate">+</button>
        </div>
    `,
    providers: [ UserListService ],
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class GroupInputUsers implements OnInit {
    @Input() model: UserModel[] = []

    private filtersUpdatesSubscriber: Subscription

    // list elements stored by store pipe in list component 
    // (takes filters in consideration)
    storedElements: UserModel[] = []
    
    // Users selected by enduser
    selectedUsers: UserModel[] = []

    structure: StructureModel
    structures: StructureModel[] = []

    constructor(private groupsStore: GroupsStore,
        public userLS: UserListService,
        private spinner: SpinnerService,
        private ns: NotifyService,
        private cdRef: ChangeDetectorRef,
        public listFilters: UserlistFiltersService) {}

    ngOnInit(): void {
        this.structure = this.groupsStore.structure
        this.structures = globalStore.structures.data

        this.filtersUpdatesSubscriber = this.listFilters.updateSubject.subscribe(() => {
            this.cdRef.markForCheck()
        })
    }

    ngOnDestroy(): void {
        this.filtersUpdatesSubscriber.unsubscribe()
    }

    selectUser(u: UserModel): void {
        if (this.selectedUsers.indexOf(u) === -1) {
            this.selectedUsers.push(u)
        } else {
            this.selectedUsers = this.selectedUsers.filter(su => su.id !== u.id)
        }
    }

    isSelected = (user: UserModel) => {
        return this.selectedUsers.indexOf(user) > -1;
    }

    selectAll(): void {
        this.selectedUsers = this.storedElements
    }

    deselectAll(): void {
        this.selectedUsers = []
    }

    structureChange(s: StructureModel): void {
        let selectedStructure: StructureModel = globalStore.structures.data.find(
            globalS => globalS.id === s.id)
        this.structure = selectedStructure

        if (selectedStructure.users && selectedStructure.users.data 
            && selectedStructure.users.data.length < 1) {
            this.spinner.perform('group-manage-users',
                selectedStructure.users.sync()
                    .then(() => {
                        this.model = selectedStructure.users.data
                            .filter(u => 
                                this.groupsStore.group.users.map(x => x.id).indexOf(u.id) === -1)
                        this.cdRef.markForCheck()
                    })
                    .catch((err) => {
                        this.ns.error(
                            {key: 'notify.structure.syncusers.error.content'
                            , parameters: {'structure': s.name}}
                            , 'notify.structure.syncusers.error.title'
                            , err)
                    })
            )
        } else {
            this.model = selectedStructure.users.data
                .filter(u => this.groupsStore.group.users.map(x => x.id).indexOf(u.id) === -1)
            this.cdRef.markForCheck()
        }
    }

    addUsers(): void {
        this.spinner.perform('group-manage-users',
            this.groupsStore.group.addUsers(this.selectedUsers)
                .then(() => {
                    this.groupsStore.group.users = this.groupsStore.group.users.concat(this.selectedUsers)
                    this.model = this.model.filter(u => this.selectedUsers.indexOf(u) === -1)
                    this.selectedUsers = []
                    this.ns.success('notify.group.manage.users.added.content')
                    this.cdRef.markForCheck()
                })
                .catch((err) => {
                    this.ns.error('notify.group.manage.users.added.error.content'
                        , 'notify.group.manage.users.added.error.title', err)
                })
        )
    }
}
