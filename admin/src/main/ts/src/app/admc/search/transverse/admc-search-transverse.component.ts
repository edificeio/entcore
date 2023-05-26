import { Component, EventEmitter, Injector, Input, Output } from "@angular/core";
import { OdeComponent } from "ngx-ode-core";
import { BundlesService } from "ngx-ode-sijil";
import { SpinnerService } from "ngx-ode-ui";
import { SearchTypeEnum, SearchTypeValue } from "src/app/core/enum/SearchTypeEnum";
import { UserListService } from "src/app/core/services/userlist.service";
import { UserModel } from "src/app/core/store/models/user.model";
import { UsersStore } from "src/app/users/users.store";
import { AdmcSearchService } from "../admc-search.service";
import { UserSearchTerms } from "../components/user-search-input/user-search-input.component";

@Component({
    selector: 'ode-admc-search-transverse',
    templateUrl: './admc-search-transverse.component.html',
    styleUrls: ['./admc-search-transverse.component.scss']
})
export class AdmcSearchTransverseComponent extends OdeComponent {
    
    nbUser: number;
    searchTerms: UserSearchTerms;
    userlist: UserModel[];
    searchTypes: Array<{label: string, value: SearchTypeEnum}>;
    selectedSearchTypeValue: SearchTypeValue;

    // Selection
    @Input() selectedUser: UserModel;
    @Output() onselect: EventEmitter<UserModel> = new EventEmitter<UserModel>();

    constructor(
        private bundles: BundlesService,
        public spinner: SpinnerService,
        public userListService: UserListService,
        private admcSearchService: AdmcSearchService,
        public usersStore: UsersStore,
        injector: Injector) {
        super(injector);
    }

    ngOnInit(): void {
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
        this.searchTerms = [];
    }

    refreshListCount(list): void {
        if (list) {
            this.nbUser = list.length;
        }
    }

    isSelected = (user: UserModel): boolean => {
        return this.selectedUser && user && this.selectedUser.id === user.id;
    }

    handleSelectSearchType(searchTypeValue: SearchTypeValue): void {
        this.selectedSearchTypeValue = searchTypeValue;
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

    get disableSearch():boolean {
        return !(
            this.searchTerms && (
                (this.searchTerms[0] && this.searchTerms[0].trim().length >= 3)
                    ||
                (this.searchTerms[1] && this.searchTerms[1].trim().length >= 3)
            )
        );
    }

    search = (): void => {
        this.spinner.perform('portal-content', 
            this.admcSearchService.search(this.searchTerms, this.selectedSearchTypeValue).then(data => {
                this.userlist = data;
                this.refreshListCount(data);
                this.changeDetector.markForCheck();
            })
        );
    }

    handleSelectUser(user: UserModel): void {
        this.selectedUser = user;
        this.usersStore.user = user;
        this.spinner.perform('portal-content', this.router.navigate([user.id, 'details'], {relativeTo: this.route}));
    }
}
