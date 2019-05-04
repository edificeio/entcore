import { Component, OnInit, OnDestroy, ChangeDetectionStrategy, ChangeDetectorRef} from '@angular/core'
import { Subscription, Subject } from 'rxjs'
import { ActivatedRoute, Router, Data, NavigationEnd } from '@angular/router'
import { routing } from '../../core/services/routing.service'
import { StructureModel } from '../../core/store'

@Component({
    selector: 'export',
    template: `
        <div class="container has-shadow">
            <h2><s5l>export.configuration</s5l></h2>

            <form class="has-vertical-padding-10">

                <form-field label="export.type">
                    <select [(ngModel)]="type" name="type">
                        <option value="">
                            <s5l>export.type.default</s5l>
                        </option>
                        <option value="Transition">
                            <s5l>export.type.transition</s5l>
                        </option>
                    </select>
                </form-field>

                <form-field label="export.classe">
                    <select [(ngModel)]="classe" name="classe">
                        <option value="">
                            <s5l>export.all.classes</s5l>
                        </option>
                        <option *ngFor="let c of structure.classes | orderBy: ['+name']" [ngValue]="c.id">
                            <s5l>{{ c.name }}</s5l>
                        </option>
                    </select>
                </form-field>

                <form-field label="profile">
                    <select class="three cell row-item" [(ngModel)]="profile" name="profile">
                        <option value="" *ngIf="type.length == 0">
                            <s5l>export.all.profiles</s5l>
                        </option>
                        <option value="Teacher">
                            <s5l>Teacher</s5l>
                        </option>
                        <option value="Personnel">
                            <s5l>Personnel</s5l>
                        </option>
                        <option value="Relative">
                            <s5l>Relative</s5l>
                        </option>
                        <option value="Student">
                            <s5l>Student</s5l>
                        </option>
                        <option value="Guest">
                            <s5l>Guest</s5l>
                        </option>
                    </select>
                </form-field>

                <form-field label="filter">
                    <select class="three cell" [(ngModel)]="filter" name="filter">
                        <option value=""><s5l>ignore.activation</s5l></option>
                        <option value="active"><s5l>users.activated</s5l></option>
                        <option value="inactive"><s5l>users.not.activated</s5l></option>
                    </select>
                </form-field>

                <button class="is-pulled-right action" [disabled]="false" (click)="launchExport()">
                    <s5l>export</s5l>
                </button>
            </form>
        </div>`,
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class ExportComponent implements OnInit{
    
    structure: StructureModel;
    type = "";
    classe = "";
    profile = "";
    filter="";
    dataSubscriber: Subscription;
    routerSubscriber: Subscription;
    
    constructor(
        public route: ActivatedRoute,
        public router: Router,
        public cdRef: ChangeDetectorRef) {}

    ngOnInit(): void {
        this.dataSubscriber = routing.observe(this.route, "data").subscribe(async (data: Data) => {
            if (data['structure']) {
                this.structure = data['structure'];
                this.cdRef.detectChanges();
            }
        })

        this.routerSubscriber = this.router.events.subscribe(e => {
            if(e instanceof NavigationEnd) {
                this.cdRef.markForCheck();
            }
        })
    }

    launchExport(): void{
        let link = `directory/export/users?format=csv&filterActive=${this.filter}`;

        if(this.classe.length > 0) {
            link = `${link}&classId=${this.classe}`;
        } else {
            link = `${link}&structureId=${this.structure._id}`;
        }

        if(this.profile.length > 0) {
            link = `${link}&profile=${this.profile}`;
        }

        if(this.type.length > 0) {
            link = `${link}&type=${this.type}`;
        }

        window.open(link, '_blank');
    }

}
