import { Component, Input, Output, ChangeDetectionStrategy, ChangeDetectorRef
    , OnInit, EventEmitter } from '@angular/core'
import { ActivatedRoute } from '@angular/router'
import { Subscription } from 'rxjs/Subscription'
import { UserListService, UserlistFiltersService, LoadingService
    , NotifyService } from '../../../../../../services'
import { GroupsStore } from '../../../../store'
import { UserModel, StructureModel, globalStore } from '../../../../../../store'

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
            <list-component
                [model]="model"
                [filters]="listFilters.getFormattedFilters()"
                [inputFilter]="userLS.filterByInput"
                [sort]="userLS.sorts"
                [ngClass]="setUserListStyles"
                (inputChange)="userLS.inputFilter = $event"
                (onSelect)="selectUser($event)">
                <div toolbar class="user-toolbar">
                    <i class="fa" aria-hidden="true"
                        [ngClass]="{
                            'fa-sort-alpha-asc': userLS.sortsMap.alphabetical.sort === '+',
                            'fa-sort-alpha-desc': userLS.sortsMap.alphabetical.sort === '-',
                            'selected': userLS.sortsMap.alphabetical.selected
                        }"
                        [tooltip]="'sort.alphabetical' | translate" position="top"
                        (click)="userLS.changeSorts('alphabetical')"></i>

                    <i class="fa" aria-hidden="true"
                        [ngClass]="{
                            'fa-sort-amount-asc': userLS.sortsMap.profile.sort === '+',
                            'fa-sort-amount-desc': userLS.sortsMap.profile.sort === '-',
                            'selected': userLS.sortsMap.profile.selected
                        }"
                        [tooltip]="'sort.profile' | translate" position="top"
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
            </list-component>

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

    private selectedUsers: UserModel[] = []

    private structure: StructureModel
    private structures: StructureModel[] = []

    constructor(private groupsStore: GroupsStore,
        private userLS: UserListService,
        private ls: LoadingService,
        private ns: NotifyService,
        private cdRef: ChangeDetectorRef,
        private listFilters: UserlistFiltersService) {}

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

    private selectUser(u: UserModel): void {
        if (this.selectedUsers.indexOf(u) === -1) {
            this.selectedUsers.push(u)
        } else {
            this.selectedUsers = this.selectedUsers.filter(su => su.id !== u.id)
        }
    }

    private setUserListStyles = (user: UserModel) => {
        return { selected: this.selectedUsers.indexOf(user) > -1 }
    }

    private selectAll(): void {
        this.selectedUsers = this.model.filter(u => this.groupsStore.group.users.map(gu => gu.id).indexOf(u.id) === -1)
    }

    private deselectAll(): void {
        this.selectedUsers = []
    }

    private structureChange(s: StructureModel): void {
        let selectedStructure: StructureModel = globalStore.structures.data.find(globalS => globalS.id === s.id)

        if (selectedStructure.users && selectedStructure.users.data 
            && selectedStructure.users.data.length < 1) {
            this.ls.perform('group-manage-users',
                selectedStructure.users.sync()
                    .then(() => {
                        this.model = selectedStructure.users.data.filter(
                            u => this.groupsStore.group.users.map(x => x.id).indexOf(u.id) === -1)
                        this.cdRef.detectChanges()
                    })
                    .catch((err) => {
                        this.ns.error(
                            {key: 'notify.structure.syncusers.error.content'
                            , parameters: {'structure': s.name}}
                            , 'notify.structure.syncusers.error.title'
                            , err)
                    })
            )
        }

        this.structure = s
    }

    private addUsers(): void {
        this.ls.perform('group-manage-users',
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
