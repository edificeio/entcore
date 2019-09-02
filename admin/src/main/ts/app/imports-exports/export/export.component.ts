import { ChangeDetectionStrategy, ChangeDetectorRef, Component, OnInit } from '@angular/core';
import { Subscription } from 'rxjs/Subscription';
import { ActivatedRoute, Data, NavigationEnd, Router } from '@angular/router';
import { routing } from '../../core/services/routing.service';
import { StructureModel } from '../../core/store';
import { SelectOption } from '../../shared/ux/components/multi-select.component';
import { OrderPipe } from '../../shared/ux/pipes';

@Component({
    selector: 'export',
    template: `
        <div class="container has-shadow">
            <h2>
                <s5l>export.configuration</s5l>
            </h2>

            <form class="has-vertical-padding-10">

                <form-field label="export.type">
                    <mono-select class="is-block" [(ngModel)]="type" name="type"
                                 [options]="[{value: '', label: 'export.type.default'}, {value: 'Transition', label: 'export.type.transition'}]">
                    </mono-select>
                </form-field>

                <form-field label="export.classe">
                    <mono-select class="is-block" [(ngModel)]="classe" name="classe" [options]="classeOptions">
                    </mono-select>
                </form-field>

                <form-field label="profile">
                    <mono-select class="is-block" [(ngModel)]="profile" name="profile"
                                 [options]="getProfileOptions()">
                    </mono-select>
                </form-field>

                <form-field label="filter">
                    <mono-select class="is-block" [(ngModel)]="filter" name="filter"
                                 [options]="[{value: '', label: 'ignore.activation'}, {value: 'active', label: 'users.activated'}, {value: 'inactive', label: 'users.not.activated'}]">
                    </mono-select>
                </form-field>

                <button class="is-pulled-right action" [disabled]="false" (click)="launchExport()">
                    <s5l>export</s5l>
                </button>
            </form>
        </div>`,
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class ExportComponent implements OnInit {

    structure: StructureModel;
    type = "";
    classe = "";
    profile = "";
    filter = "";

    public classeOptions: SelectOption<string>[] = [];
    public profileOptions: SelectOption<string>[] = ['Teacher', 'Personnel', 'Relative', 'Student', 'Guest'].map(t => ({
        value: t,
        label: t
    }));
    public profileOptionsWithAllProfiles: SelectOption<string>[] = [{
        value: '',
        label: 'export.all.profiles'
    }].concat(this.profileOptions);
    public getProfileOptions(): SelectOption<string>[] {
        if (this.type.length === 0) {
            return this.profileOptionsWithAllProfiles;
        }
        if (this.profile.length === 0) {
            this.profile = 'Teacher';
        }
        return this.profileOptions;
    };

    dataSubscriber: Subscription;
    routerSubscriber: Subscription;

    constructor(
        public route: ActivatedRoute,
        private orderPipe: OrderPipe,
        public router: Router,
        public cdRef: ChangeDetectorRef) {
    }

    ngOnInit(): void {
        this.dataSubscriber = routing.observe(this.route, "data").subscribe(async (data: Data) => {
            if (data['structure']) {
                this.structure = data['structure'];
                this.classeOptions = [{value: '', label: 'export.all.classes'}].concat(
                    this.orderPipe.transform(this.structure.classes, '+name')
                        .map(classe => ({value: classe.id, label: classe.name})
                        )
                );
                this.cdRef.detectChanges();
            }
        })

        this.routerSubscriber = this.router.events.subscribe(e => {
            if (e instanceof NavigationEnd) {
                this.cdRef.markForCheck();
            }
        })
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
