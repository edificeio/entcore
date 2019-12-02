import { OdeComponent } from './../../core/ode/OdeComponent';
import { ChangeDetectionStrategy, ChangeDetectorRef, Component, OnInit, Injector } from '@angular/core';
import {Subscription} from 'rxjs';
import {ActivatedRoute, Data, NavigationEnd, Router} from '@angular/router';
import {routing} from '../../core/services/routing.service';
import {StructureModel} from '../../core/store/models/structure.model';
import {SelectOption} from '../../shared/ux/components/multi-select/multi-select.component';
import {OrderPipe} from '../../shared/ux/pipes';

@Component({
    selector: 'ode-export',
    templateUrl: './export.component.html',
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class ExportComponent extends OdeComponent implements OnInit {


    constructor(
        injector: Injector,
        private orderPipe: OrderPipe) {
            super(injector);
    }

    structure: StructureModel;
    type = '';
    classe = '';
    profile = '';
    filter = '';

    public classeOptions: SelectOption<string>[] = [];
    public profileOptions: SelectOption<string>[] = ['Teacher', 'Personnel', 'Relative', 'Student', 'Guest'].map(t => ({
        value: t,
        label: t
    }));
    public profileOptionsWithAllProfiles: SelectOption<string>[] = [{
        value: '',
        label: 'export.all.profiles'
    }].concat(this.profileOptions);

    dataSubscriber: Subscription;
    routerSubscriber: Subscription;
    public getProfileOptions(): SelectOption<string>[] {
        if (this.type.length === 0) {
            return this.profileOptionsWithAllProfiles;
        }
        if (this.profile.length === 0) {
            this.profile = 'Teacher';
        }
        return this.profileOptions;
    }
    ngOnInit(): void {
        super.ngOnInit();
        this.dataSubscriber = routing.observe(this.route, 'data').subscribe(async (data: Data) => {
            if (data.structure) {
                this.structure = data.structure;
                this.classeOptions = [{value: '', label: 'export.all.classes'}].concat(
                    this.orderPipe.transform(this.structure.classes, '+name')
                        .map(classe => ({value: classe.id, label: classe.name})
                        )
                );
                this.changeDetector.detectChanges();
            }
        });

        this.routerSubscriber = this.router.events.subscribe(e => {
            if (e instanceof NavigationEnd) {
                this.changeDetector.markForCheck();
            }
        });
    }

    launchExport(): void {
        let link = `directory/export/users?format=csv&filterActive=${this.filter}`;

        if (this.classe.length > 0) {
            link = `${link}&classId=${this.classe}`;
        } else {
            link = `${link}&structureId=${this.structure._id}`;
        }

        if (this.profile.length > 0) {
            link = `${link}&profile=${this.profile}`;
        }

        if (this.type.length > 0) {
            link = `${link}&type=${this.type}`;
        }

        window.open(link, '_blank');
    }

}
