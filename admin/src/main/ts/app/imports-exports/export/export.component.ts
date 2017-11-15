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

            <form class="has-vertical-padding">
                <form-field label="export.classe">
                    <select [(ngModel)]="classe" name="classe">
                        <option value="">
                            <s5l>all</s5l>
                        </option>
                        <option *ngFor="let c of structure.classes | orderBy: ['+name']" [ngValue]="c.id">
                            <s5l>{{ c.name }}</s5l>
                        </option>
                    </select>
                </form-field>

                <form-field label="profile">
                    <select class="three cell row-item" [(ngModel)]="profile" name="profile">
                        <option value="">
                            <s5l>all</s5l>
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

        window.open(link, '_blank');
    }
}
