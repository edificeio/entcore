import { AfterViewChecked, ChangeDetectionStrategy, Component, EventEmitter, Injector, Input, OnDestroy, OnInit, Output } from '@angular/core';
import { OdeComponent } from 'ngx-ode-core';
import { UserListService } from 'src/app/core/services/userlist.service';
import { UserModel } from '../../core/store/models/user.model';
import { UsersService } from '../users.service';
import { StructureModel } from 'src/app/core/store/models/structure.model';
import { BundlesService } from 'ngx-ode-sijil';
import { SpinnerService } from 'ngx-ode-ui';
import { SearchTypeEnum } from 'src/app/core/enum/SearchTypeEnum';

@Component({
    selector: 'ode-tree-user-list',
    templateUrl: './tree-user-list.component.html',
    styleUrls: ['./tree-user-list.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class TreeUserListComponent extends OdeComponent implements OnInit, OnDestroy, AfterViewChecked {

    nbUser: number;
    searchTerm: string;
    searchTypes: Array<{label: string, value: SearchTypeEnum}>;
    selectedSearchTypeValue: string;
    userlist: UserModel[];

    @Input()
    structure: StructureModel;

    // Selection
    @Input() selectedUser: UserModel;
    @Output() onselect: EventEmitter<UserModel> = new EventEmitter<UserModel>();

    constructor(
        private bundles: BundlesService,
        public usersService: UsersService,
        public userListService: UserListService,
        public spinner: SpinnerService,
        injector: Injector) {
            super(injector);
        }

    ngOnInit() {
        super.ngOnInit();
        this.nbUser = this.userlist ? this.userlist.length: 0;
        this.searchTypes = [
            {
                label: 'user.searchType.name',
                value: SearchTypeEnum.DISPLAY_NAME
            },
            {
                label: 'user.searchType.email',
                value: SearchTypeEnum.EMAIL
            }
        ];
        this.selectedSearchTypeValue = SearchTypeEnum.DISPLAY_NAME;
    }

    ngAfterViewChecked() {
        // called to update list nbUser after filters update
        this.changeDetector.markForCheck();
    }

    isSelected = (user: UserModel): boolean => {
        return this.selectedUser && user && this.selectedUser.id === user.id;
    }

    refreshListCount(list): void {
        if (list) {
            this.nbUser = list.length;
        }
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

    handleSelectSearchType(searchTypeValue: string): void {
        this.selectedSearchTypeValue = searchTypeValue;
    }

    search = (): void => {
        this.spinner.perform('portal-content',
            this.usersService.search(this.searchTerm, this.selectedSearchTypeValue, this.structure.id).then(data => {
                this.userlist = data;
                this.refreshListCount(data);
                this.changeDetector.markForCheck();
            })
        );
    }
}
