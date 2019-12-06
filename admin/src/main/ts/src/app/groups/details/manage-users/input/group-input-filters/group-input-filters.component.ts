import { ChangeDetectionStrategy, Component, ElementRef, EventEmitter, Injector, Input, OnChanges, Output } from '@angular/core';
import { OdeComponent } from 'ngx-ode-core';
import { BundlesService } from 'ngx-ode-sijil';
import { ProfilesService } from 'src/app/core/services/profiles.service';
import { UserlistFiltersService } from 'src/app/core/services/userlist.filters.service';
import { StructureModel } from '../../../../../core/store/models/structure.model';


@Component({
    selector: 'ode-group-input-filters-users',
    templateUrl: './group-input-filters.component.html',
    host: {
        '(document:click)': 'onClick($event)',
    },
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class GroupInputFiltersComponent extends OdeComponent implements OnChanges {

    @Input()
    private structure: StructureModel;

    @Output()
    private onOpen = new EventEmitter<any>();

    @Output()
    private onClose = new EventEmitter<any>();

    show = false;

    private deselectItem = false;

    constructor(
        private _eref: ElementRef,
        private bundles: BundlesService,
        public listFilters: UserlistFiltersService,
        injector: Injector) {
            super(injector);
        }

    ngOnChanges(): void {
        this.initFilters();
    }

    translate = (...args) => (this.bundles.translate as any)(...args);

    private initFilters() {
        this.listFilters.resetFilters();

        this.structure.syncClasses().then(() => {
            this.listFilters.setClassesComboModel(this.structure.classes);
            this.changeDetector.markForCheck();
        });
        this.structure.syncAafFunctions().then(() => {
            const aafFunctions: Array<Array<string>> = [];
            this.structure.aafFunctions.forEach(f => {
                f.forEach(f2 => aafFunctions.push([f2[2], f2[4]]));
            });
            this.listFilters.setFunctionsComboModel(aafFunctions);
            this.changeDetector.markForCheck();
        });
        ProfilesService.getProfiles().then(p => {
            this.structure.profiles = p;
            this.listFilters.setProfilesComboModel(this.structure.profiles.map(p => p.name));
            this.changeDetector.markForCheck();
        });
        this.structure.groups.sync().then(() => {
            this.listFilters.setFunctionalGroupsComboModel(
                this.structure.groups.data.filter(g => g.type === 'FunctionalGroup').map(g => g.name));
            this.listFilters.setManualGroupsComboModel(
                this.structure.groups.data.filter(g => g.type === 'ManualGroup').map(g => g.name));
            this.changeDetector.markForCheck();
        });
        this.listFilters.setMailsComboModel([]);
    }

    orderer(a) {
        return a;
    }

    deselect(filter, item) {
        filter.outputModel.splice(filter.outputModel.indexOf(item), 1);
        this.deselectItem = true;
    }

    onClick(event) {
        if (this.show && !this._eref.nativeElement.contains(event.target) && !this.deselectItem) {
            this.toggleVisibility();
        }
        this.deselectItem = false;
        return true;
    }

    toggleVisibility(): void {
        this.show = !this.show;
    }

    filtersOn(): boolean {
        return this.listFilters.filters.some(f => f.outputModel && f.outputModel.length > 0);
    }
}
