import { Component, Injector, Input, OnInit } from "@angular/core";
import { OdeComponent } from "ngx-ode-core";
import { SpinnerService } from "ngx-ode-ui";
import { ListSearchParameters, UnlinkedUser, UnlinkedUserService } from "./unlinked.service";
import { SearchTypeEnum } from "src/app/core/enum/SearchTypeEnum";
import { UnlinkedUserDetailsComponent } from "./details/user-details.component";
import { Subscription } from "rxjs";

@Component({
    selector: 'ode-admc-unlinked',
    templateUrl: './admc-search-unlinked.component.html'
})
export class AdmcSearchUnlinkedComponent extends OdeComponent implements OnInit {
    @Input() selectedItem: UnlinkedUser;
    private ondeleteUserSubscription?:Subscription;
    public collectionRef: Array<UnlinkedUser> = [];
    // search parameters
    public itemInputFilter:string;
    private noMoreResults = false;
    searchTypes: Array<{label: string, value: SearchTypeEnum}>;
    selectedSearchTypeValue: SearchTypeEnum;

    constructor(injector: Injector, 
        private svc:UnlinkedUserService, 
        private spinner: SpinnerService
        ) {
        super(injector);
    }

    ngOnInit(): void {
        super.ngOnInit();
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

    /** Called when the angular router displays the details of the selected user. */
    onUserDetailsAttached(e) {
        if (e && e instanceof UnlinkedUserDetailsComponent) {
            const detailsComponent = e as UnlinkedUserDetailsComponent;
            this.ondeleteUserSubscription = detailsComponent.ondelete.asObservable().subscribe({
                next: (e)=> { 
                    const idx = this.collectionRef.findIndex( u => u.id===this.selectedItem.id );
                    if( idx >= 0 ) {
                        this.collectionRef.splice( idx, 1 );
                        this.collectionRef = [].concat(this.collectionRef); // Only way for updating the list ?!
                        this.selectedItem = null;
                        this.router.navigate(['.'], {relativeTo: this.route});
                    }
                }
            });
        }
    }
    /** Called when angular hides the details of the selected user. */
    onUserDetailsDetached(e) {
        if( this.ondeleteUserSubscription ) {
            this.ondeleteUserSubscription.unsubscribe();
            delete this.ondeleteUserSubscription;
        }
    }

    onSearchTermChanged(value:string) {
        this.itemInputFilter = value;
        this.noMoreResults = true;
    }

    get nbUsers(): string {
        return `${this.collectionRef.length}${(this.collectionRef.length===0 || this.noMoreResults)?'':'+'}`;
    }

    search = async () => {
        const PAGE_SIZE = 50;

        if( this.noMoreResults ) {
            // Reset a new search
            this.noMoreResults = false;
            this.collectionRef = [];
        }
        const params:ListSearchParameters = {
            sortOn: "+displayName",
            fromIndex: this.collectionRef.length,
            limitResult: PAGE_SIZE,
            searchType: this.selectedSearchTypeValue,
            searchTerm: this.itemInputFilter
        }
        this.collectionRef = this.collectionRef.concat( await this.spinner.perform( 'portal-content', 
            this.svc.list(params))
            .then( users => {
                if( users.length < PAGE_SIZE ) {
                    this.noMoreResults = true;
                }
                return users;
            })
            .then( users => users.filter(u=>!!u.displayName) )
        );
    }

    closePanel(): void {
        this.router.navigate(['..'], {relativeTo: this.route});
    }

    showCompanion = (): boolean => {
        return true;
/*
        const basePath = `/admin/admc/apps/roles`;
        if (this.selectedUser) {
            return this.router.isActive(`${basePath}\\${this.selectedItem.id}`, true);
        } else {
            return false;
        }
*/
    }

    filterByInput = (item: any): boolean => {
        return !!this.itemInputFilter && item.displayName
            ? item.displayName.toLowerCase().indexOf(this.itemInputFilter.toLowerCase()) >= 0 
            : true;
    }

    isSelected = (item): boolean => {
        return this.selectedItem && item && this.selectedItem.id === item.id;
    }

    async onSelectItem( user:UnlinkedUser ) {
        this.selectedItem = user;
        this.router.navigate([user.id, 'details'], {relativeTo: this.route});
    }

    handleSelectSearchType(searchTypeValue: SearchTypeEnum): void {
        this.selectedSearchTypeValue = searchTypeValue;
    }
}
