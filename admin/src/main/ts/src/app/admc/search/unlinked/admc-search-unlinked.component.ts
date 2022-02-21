import { Component, Injector, Input, OnInit } from "@angular/core";
import { OdeComponent } from "ngx-ode-core";
import { UnlinkedUser } from "./unlinked.service";

@Component({
    selector: 'ode-admc-unlinked',
    templateUrl: './admc-search-unlinked.component.html'
})
export class AdmcSearchUnlinkedComponent extends OdeComponent implements OnInit {
    @Input() selectedItem: UnlinkedUser;
    public collectionRef: Array<UnlinkedUser>;
    public itemInputFilter:string;

    constructor(injector: Injector) {
        super(injector);
    }

    ngOnInit(): void {
        super.ngOnInit();

        // Get the initial unlinked users from the route resolver.
        const users:Array<UnlinkedUser> = this.route.snapshot.data.unlinked;

        this.collectionRef = users.sort((a, b) => {
            return (a && a.displayName && b && b.displayName) ? a.displayName.localeCompare(b.displayName) : 0;
        });
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
}
