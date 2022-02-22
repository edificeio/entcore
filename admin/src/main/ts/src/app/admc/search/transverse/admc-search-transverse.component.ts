import { Component, EventEmitter, Injector, Input, Output } from "@angular/core";
import { OdeComponent } from "ngx-ode-core";
import { BundlesService } from "ngx-ode-sijil";
import { SpinnerService } from "ngx-ode-ui";
import { SearchTypeEnum } from "src/app/core/enum/SearchTypeEnum";
import { UserListService } from "src/app/core/services/userlist.service";
import { UserModel } from "src/app/core/store/models/user.model";
import { AdmcSearchService } from "../admc-search.service";

@Component({
    selector: 'ode-admc-search-transverse',
    templateUrl: './admc-search-transverse.component.html',
    styleUrls: ['./admc-search-transverse.component.scss']
})
export class AdmcSearchTransverseComponent extends OdeComponent {
    
    nbUser: number;
    searchTerm: string;
    userlist: UserModel[];
    searchTypes: Array<{label: string, value: SearchTypeEnum}>;
    selectedSearchTypeValue: string;

    // Selection
    @Input() selectedUser: UserModel;
    @Output() onselect: EventEmitter<UserModel> = new EventEmitter<UserModel>();

    constructor(
        private bundles: BundlesService,
        public spinner: SpinnerService,
        public userListService: UserListService,
        private admcSearchService: AdmcSearchService,
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
    }

    refreshListCount(list): void {
        if (list) {
            this.nbUser = list.length;
        }
    }

    isSelected = (user: UserModel): boolean => {
        return this.selectedUser && user && this.selectedUser.id === user.id;
    }

    handleSelectSearchType(searchTypeValue: string): void {
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

    search = (): void => {
        this.spinner.perform('portal-content', 
            this.admcSearchService.search(this.searchTerm, this.selectedSearchTypeValue).then(data => {
                this.userlist = data;
                this.refreshListCount(data);
                this.changeDetector.markForCheck();
            })
        );
    }
}