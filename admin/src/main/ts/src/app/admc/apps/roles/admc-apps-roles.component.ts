import { Component, Injector, Input, OnInit } from '@angular/core';
import { OdeComponent } from 'ngx-ode-core';
import { InputFileService } from 'ngx-ode-ui';
import { ApplicationModel } from 'src/app/core/store/models/application.model';
import { AdmcAppsRolesService } from './admc-apps-roles.service';

@Component({
    selector: 'ode-admc-apps-roles-root',
    templateUrl: './admc-apps-roles.component.html',
    providers: [AdmcAppsRolesService]
})
export class AdmcAppsRolesComponent extends OdeComponent implements OnInit {
    constructor(
        injector: Injector,
        public inputFileService: InputFileService,
        protected roleSvc: AdmcAppsRolesService
        ) {
            super(injector);
    }

    @Input() selectedItem: ApplicationModel;

    public collectionRef: Array<ApplicationModel>;
    private itemInputFilter: string;

    ngOnInit(): void {
        super.ngOnInit();

        // Get the apps from the route resolver.
        const apps:Array<ApplicationModel> = this.route.snapshot.data.apps;

        this.collectionRef = apps.sort((a, b) => {
            return (a && a.displayName && b && b.displayName) ? a.displayName.localeCompare(b.displayName) : 0;
        });
    }

    closePanel(): void {
        this.router.navigate(['..'], {relativeTo: this.route});
    }

    showCompanion = (): boolean => {
        const basePath = `/admin/admc/apps/roles`;
        if (this.selectedItem) {
            return this.router.isActive(`${basePath}\\${this.selectedItem.id}`, true);
        } else {
            return false;
        }
    }

    filterByInput = (item: any): boolean => {
        return !!this.itemInputFilter 
            ? item.displayName.toLowerCase().indexOf(this.itemInputFilter.toLowerCase()) >= 0 
            : true;
    }

    isSelected = (item): boolean => {
        return this.selectedItem && item && this.selectedItem.id === item.id;
    }
}
