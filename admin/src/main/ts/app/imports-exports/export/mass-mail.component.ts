import { Component, OnInit, OnDestroy,ChangeDetectionStrategy, ChangeDetectorRef } from '@angular/core'
import { Subscription, Subject } from 'rxjs'
import { ActivatedRoute, Router, Data, NavigationEnd } from '@angular/router'
import { routing } from '../../core/services/routing.service'
import { UserlistFiltersService, UserFilter } from '../../core/services/userlist.filters.service'
import { StructureModel, UserModel } from '../../core/store'
import { MassMailService } from './mass-mail.service'
import { FiltersPipe } from './mass-mail.pipe'


@Component({
    selector: 'mass-mail',
    template: `
            <side-layout 
                [showCompanion]="true">
            <div side-card>
                      <div class="padded">
            <div *ngFor="let filter of listFilters.filters">
                <div *ngIf="filter.comboModel.length > 0">
                    <multi-combo
                        [comboModel]="filter.comboModel"
                        [(outputModel)]="filter.outputModel"
                        [title]="filter.label | translate"
                        [display]="filter.display || translate"
                        [orderBy]="filter.order || orderer"
                        [filter]="check()"
                    ></multi-combo>
                    <div class="multi-combo-companion">
                        <div *ngFor="let item of filter.outputModel"
                            (click)="deselect(filter, item)">
                            <span *ngIf="filter.display">
                                {{ item[filter.display] }}
                            </span>
                            <span *ngIf="!filter.display">
                                {{ item | translate }}
                            </span>
                            <i class="fa fa-trash" (click)="check()"></i>
                        </div>
                    </div>
                </div>
            </div>
            <div *ngFor="let filter of listFilters.filters">
                <div *ngIf="filter.comboModel.length > 0">
                    
                </div>
            </div>
            </div>
            </div>
            <div side-companion class="padded">
                <a>Lancer le publipostage : TO DO </a>
            <button class="cell" ng-click="massmail.process('pdf')">
                <s5l>directory.massmail.pdf</s5l>
            </button>
            <button class="cell" ng-click="">
                <s5l>directory.massmail.mail</s5l>
            </button>
            </div>

            <div side-companion>
            
            <hr>
               
            <table class="" ng-if="!massmail.processing && !massmail.fetchingUsers">
                <thead>
                    <tr>
                        <th (click)="setUserOrder('lastName')"><s5l>lastName</s5l></th>
                        <th (click)="setUserOrder('firstName')"><s5l>firstName</s5l></th>
                        <th (click)="setUserOrder('profile')"><s5l>profile</s5l></th>
                        <th (click)="setUserOrder('login')"><s5l>login</s5l></th>
                        <th (click)="setUserOrder('activationCode')"><s5l>activation.code</s5l></th>
                        <th (click)="setUserOrder('email')"><s5l>email</s5l></th>
                        <th (click)="setUserOrder('classesStr')"><s5l>create.user.classe</s5l></th>
                        <th>link</th>
                    </tr>
                    <tr>    
                        <th>
                            <input class="twelve" type="text" ng-model="massmail.sortObject.lastName" placeholder="TO DO"/></th>
                        <th>
                            <input type="text" ng-model="massmail.sortObject.firstName" placeholder="TO DO"/>
                        </th>
                        <th>
                            <input type="text" ng-model="massmail.sortObject.translatedProfile" placeholder="TO DO"/>
                        </th>
                        <th></th>
                        <th></th>
                        <th></th>
                        <th>
                            <input type="text" ng-model="massmail.sortObject.classesStr" placeholder="TO DO"/>
                        </th>
                    </tr>
                </thead>
                <tbody>
                    <tr *ngFor="let user of (data | filtersPipe: filters:dummy) | orderBy: userOrder">
                        <td>{{user.lastName}}</td>
                        <td>{{user.firstName}}</td>
                        <td>{{user.profile | translate}}</td>
                        <td>{{user.login}}</td>
                        <td>{{user.activationCode}}</td>
                        <td>{{user.email}}</td>
                        <td>{{user.classname}}
                        </td>
                        <td>
                            <a  [routerLink]="'/admin/'+structureId+'/users/'+user.id"
                            routerLinkActive="active">
                                <button  class="fa fa-external-link"></button>
                            </a>
                        </td>
                
                </tbody>
</table>
            </div>
        </side-layout>`,
    changeDetection: ChangeDetectionStrategy.OnPush


})
export class MassMailComponent implements OnInit, OnDestroy {

    constructor(
        public route: ActivatedRoute,
        public router: Router,
        public listFilters: UserlistFiltersService,
        public cdRef: ChangeDetectorRef,
        public filtersPipe: FiltersPipe
    ) {   }

    dataSubscriber: Subscription
    routerSubscriber: Subscription
    structureId;
    filters = this.listFilters.getFormattedFilters();
    data;
    userOrder;
    addFilter;
    dummy=0;
    ngOnInit(): void {
        this.dataSubscriber = routing.observe(this.route, "data").subscribe(async (data: Data) => {
            console.log(data);
            if (data['structure']) {
                let structure: StructureModel = data['structure']

                await this.getList(structure._id)
                this.structureId = structure._id;
                this.initFilters(structure)
                console.log(this.listFilters)
                this.cdRef.detectChanges();


            }
        })

        this.routerSubscriber = this.router.events.subscribe(e => {
            if(e instanceof NavigationEnd)
                this.cdRef.markForCheck()
        })
    }

    ngOnDestroy(): void {
        this.dataSubscriber.unsubscribe()
        this.routerSubscriber.unsubscribe()
    }

    private initFilters(structure: StructureModel) {
        this.listFilters.resetFilters()
        if(!this.addFilter){
            this.listFilters.pushNewFilter(new MailFilter(new Subject<any>()))
            this.addFilter = true
        }
        this.listFilters.setClasses(structure.classes)
        this.listFilters.setProfiles(structure.profiles.map(p => p.name))
    }

    private async getList(id){
        this.data = await MassMailService.getList(id);
        console.log(this.data)
    }

    private deselect(filter, item) {
        filter.outputModel.splice(filter.outputModel.indexOf(item), 1)
        filter.observable.next()
    }

    setUserOrder(order:string){
        this.userOrder=this.userOrder === order ? '-' + order : order;
    }

    check(){
        console.log("test");
        this.dummy++;
    }


}

export class MailFilter extends UserFilter<string> {
    type = 'code'
    label = 'email'
    comboModel = [ 'users.activated', 'users.not.activated' ]

    filter = (code: string) => {
        let outputModel = this.outputModel
        return outputModel.length === 0 ||
            outputModel.indexOf('users.activated') >= 0 && !code ||
            outputModel.indexOf('users.not.activated') >= 0 && !(!code)
    }
}
