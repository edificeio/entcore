import { AfterViewChecked, ChangeDetectionStrategy, Component, EventEmitter, Injector, Input, OnDestroy, OnInit, Output } from '@angular/core';
import { Data } from '@angular/router';
import { OdeComponent } from 'ngx-ode-core';
import { UserlistFiltersService } from 'src/app/core/services/userlist.filters.service';
import { UserListService } from 'src/app/core/services/userlist.service';
import { UserModel } from '../../core/store/models/user.model';
import { UsersService } from '../users.service';
import { UsersStore } from '../users.store';
import { routing } from '../../core/services/routing.service';
import { StructureModel } from 'src/app/core/store/models/structure.model';
import { BundlesService } from 'ngx-ode-sijil';
import { SpinnerService } from 'ngx-ode-ui';

@Component({
    selector: 'ode-tree-user-list',
    templateUrl: './tree-user-list.component.html',
    styleUrls: ['./tree-user-list.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class TreeUserListComponent extends OdeComponent implements OnInit, OnDestroy, AfterViewChecked {

    nbUser: number;
    searchTerm: string;
    structure: StructureModel;

    @Input() userlist: UserModel[] = [];

    @Input() listCompanion: string;
    @Output() companionChange: EventEmitter<string> = new EventEmitter<string>();

    // Selection
    @Input() selectedUser: UserModel;
    @Output() onselect: EventEmitter<UserModel> = new EventEmitter<UserModel>();

    constructor(
        private bundles: BundlesService,
        private usersStore: UsersStore,
        public usersService: UsersService,
        public userListService: UserListService,
        public listFiltersService: UserlistFiltersService,
        public spinner: SpinnerService,
        injector: Injector) {
            super(injector);
        }

    ngOnInit() {
        super.ngOnInit();
        routing.observe(this.route, 'data').subscribe(async (data: Data) => {
            if (data.structure) {
                this.structure = data.structure;
            }
        });
        this.subscriptions.add(this.listFiltersService.$updateSubject.subscribe(() => this.changeDetector.markForCheck()));
        this.subscriptions.add(this.userListService.$updateSubject.subscribe(() => this.changeDetector.markForCheck()));
        this.subscriptions.add(this.usersStore.$onchange.subscribe((field) => {
            if (field === 'user') {
                this.changeDetector.markForCheck();
            }
        }));
        this.nbUser = this.userlist.length;
    }

    ngAfterViewChecked() {
        // called to update list nbUser after filters update
        this.changeDetector.markForCheck();
    }

    isSelected = (user: UserModel): boolean => {
        return this.selectedUser && user && this.selectedUser.id === user.id;
    }

    refreshListCount(list): void {
        this.nbUser = list.length;
    }

    filtersOn(): boolean {
        return this.listFiltersService.filters.some(f => f.outputModel && f.outputModel.length > 0);
    }

    isFilterSelected(): boolean {
        return this.router.url.indexOf('/users/filter') > -1;
    }

    userStructures(item: UserModel) {
        if(item.structures && item.structures.length > 0) {
            let result = item.structures[0].name;
            if (item.structures.length > 1) {
                result += ` + ${item.structures.length-1} ${this.bundles.translate("others")}`;
            }
            return result;
        }
        return "";
    }

    search(): void {
        this.spinner.perform('portal-content',
            this.usersService.search(this.structure.id, this.searchTerm).then(data => {
                this.userlist = data;
                this.refreshListCount(data);
                this.changeDetector.markForCheck();
            })
        );
    }
}
