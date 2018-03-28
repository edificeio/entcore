import { Component, OnInit, OnDestroy, ChangeDetectionStrategy, ChangeDetectorRef, ViewChild, ElementRef } from '@angular/core'
import { Subscription, Subject } from 'rxjs'
import { ActivatedRoute, Router, Data, NavigationEnd } from '@angular/router'
import { routing } from '../../core/services/routing.service'
import { UserlistFiltersService, UserFilter } from '../../core/services/userlist.filters.service'
import { NotifyService } from '../../core/services/notify.service'
import { SpinnerService } from '../../core/services/spinner.service'
import { StructureModel, UserModel } from '../../core/store'
import { MassMailService } from './mass-mail.service'
import { BundlesService } from 'sijil'
import { FilterPipe } from '../../shared/ux/pipes'

@Component({
    selector: 'mass-mail',
    template: `
        <div class="container has-shadow">
            <h2>{{ 'massmail.accounts' | translate }}</h2>
            
            <div class="has-vertical-padding is-pulled-left">
                <button id="buttonToggle" (click)="toggleVisibility()" [ngClass]="setFiltersOnStyle()" #filtersToggle>
                    <s5l>massmail.filters</s5l> 
                    <i class="fa fa-filter filters-toggle"></i>
                </button>
                
                <div [hidden]="!show" class="filters" #filtersDiv>
                    <i class="fa fa-close close" (click)="show=false"></i>

                    <div *ngFor="let filter of listFilters.filters">
                        <div *ngIf="filter.comboModel.length > 0">
                            <multi-combo
                                [comboModel]="filter.comboModel"
                                [(outputModel)]="filter.outputModel"
                                [title]="filter.label | translate"
                                [display]="filter.display || translate"
                                [orderBy]="filter.order || orderer">
                            </multi-combo>
                            
                            <div class="multi-combo-companion">
                                <div *ngFor="let item of filter.outputModel" (click)="deselect(filter, item)">
                                    <span *ngIf="filter.display">{{ item[filter.display] }}</span>
                                    <span *ngIf="!filter.display">{{ item | translate }}</span>
                                    <i class="fa fa-trash is-size-5"></i>
                                </div>
                            </div>
                        </div>
                    </div>
                </div>
            </div>
            
            <div class="has-vertical-padding is-pulled-right">
                <a><s5l>process.massmail</s5l> : </a>
                
                <button class="cell" (click)="processMassMail('pdf')"><s5l>massmail.pdf</s5l></button>
                <button class="cell" (click)="processMassMail('mail')"><s5l>massmail.mail</s5l></button>
            </div>
            
            <div class="has-vertical-padding is-clearfix">
                <div class="message is-info">
                    <div class="message-body has-text-centered">{{countUsers}} <s5l>massmail.users.total</s5l></div>
                </div>
                <div class="message is-warning">
                    <div class="message-body has-text-centered">{{countUsersWithoutMail}} <s5l>massmail.users.nomail</s5l></div>
                </div>
            </div>

            <div class="has-vertical-padding">
                <table>
                    <thead>
                        <tr>
                            <th (click)="setUserOrder('lastName')"><i class="fa fa-sort"></i><s5l>lastName</s5l></th>
                            <th (click)="setUserOrder('firstName')"><i class="fa fa-sort"></i><s5l>firstName</s5l></th>
                            <th (click)="setUserOrder('type')"><i class="fa fa-sort"></i><s5l>profile</s5l></th>
                            <th (click)="setUserOrder('login')"><i class="fa fa-sort"></i><s5l>login</s5l></th>
                            <th (click)="setUserOrder('code')"><i class="fa fa-sort"></i><s5l>activation.code</s5l></th>
                            <th (click)="setUserOrder('email')"><i class="fa fa-sort"></i><s5l>email</s5l></th>
                            <th (click)="setUserOrder('classesStr')"><i class="fa fa-sort"></i><s5l>create.user.classe</s5l></th>
                        </tr>
                        <tr>    
                            <th>
                                <input class="twelve" type="text" [(ngModel)]="sortObject.lastName" 
                                    [attr.placeholder]="'search' | translate"/>
                            </th>
                            <th>
                                <input type="text" [(ngModel)]="sortObject.firstName" 
                                    [attr.placeholder]="'search' | translate"/>
                            </th>
                            <th colspan="4"></th>
                            <th>
                                <input type="text" [(ngModel)]="sortObject.classesStr" 
                                    [attr.placeholder]="'search' | translate"/>
                            </th>
                        </tr>
                    </thead>
                    <tbody>
                        <tr *ngFor="let user of (getData() | filter: sortObject) | orderBy: userOrder "
                            [routerLink]="'/admin/'+structureId+'/users/'+user.id" 
                            routerLinkActive="active"
                            title="{{ 'massmail.link.user' | translate}}">
                            <td> 
                                <i class="fa fa-lock" 
                                    *ngIf="user?.code && user?.code?.length > 0"
                                    title="{{ 'user.icons.tooltip.inactive' | translate }}"></i> {{user.lastName}}
                            </td>
                            <td>{{user.firstName}}</td>
                            <td [ngClass]="user.type">{{user.type | translate}}</td>
                            <td>{{user.login}}</td>
                            <td>{{user.code}}</td>
                            <td title="{{user.email}}">{{user.email}}</td>
                            <td>{{user.classesStr}}</td>
                        </tr>
                    </tbody>
                </table>
            </div>
        </div>`,
    host: {
        '(document:click)': 'onClick($event)',
    },
    changeDetection: ChangeDetectionStrategy.OnPush


})
export class MassMailComponent implements OnInit, OnDestroy {

    constructor(
        public route: ActivatedRoute,
        public router: Router,
        public listFilters: UserlistFiltersService,
        public cdRef: ChangeDetectorRef,
        public bundles: BundlesService,
        private ns: NotifyService,
        private spinner: SpinnerService
    ) { }
    sortObject = {lastName: '', firstName: '', classesStr: ''};
    dataSubscriber: Subscription
    routerSubscriber: Subscription
    countUsers = 0;
    countUsersWithoutMail = 0;
    structureId;
    filters;
    data;
    userOrder;
    addFilter = false;
    downloadAnchor = null;
    downloadObjectUrl = null;
    show: boolean = false;
    
    @ViewChild('filtersDiv') filtersDivRef: ElementRef;
    @ViewChild('filtersToggle') filtersToggleRef;
    private deselectItem: boolean = false

    translate = (...args) => { return (<any>this.bundles.translate)(...args) }

    ngOnInit(): void {
        this.dataSubscriber = routing.observe(this.route, "data").subscribe(async (data: Data) => {
            if (data['structure']) {
                let structure: StructureModel = data['structure']
                this.spinner.perform('portal-content', this.getList(structure._id))
                    .then(() => {
                        this.structureId = structure._id;
                        this.initFilters(structure)
                        this.filters = this.listFilters.getFormattedFilters();
                        this.cdRef.detectChanges();
                    }).catch(err => {
                        this.ns.error("massmail.error", "error", err);
                    })
            }
        })

        this.routerSubscriber = this.router.events.subscribe(e => {
            if (e instanceof NavigationEnd)
                this.cdRef.markForCheck()
        })
    }

    ngOnDestroy(): void {
        this.dataSubscriber.unsubscribe()
        this.routerSubscriber.unsubscribe()
    }

    private initFilters(structure: StructureModel) {
        this.listFilters.resetFilters()
        if (this.listFilters.filters.length < 8) {
            this.listFilters.pushNewFilter(new MailFilter(new Subject<any>()))
            this.addFilter = true
        }
        this.listFilters.setClasses(structure.classes)
        this.listFilters.setProfiles(structure.profiles.map(p => p.name))
    }

    private createDownloadAnchor() {
        this.downloadAnchor = document.createElement('a');
        this.downloadAnchor.style = "display: none";
        document.body.appendChild(this.downloadAnchor);
    }

    private ajaxDownload(blob, filename) {
        if (window.navigator.msSaveOrOpenBlob) {
            //IE specific
            window.navigator.msSaveOrOpenBlob(blob, filename);
        } else {
            //Other browsers
            if (this.downloadAnchor === null)
                this.createDownloadAnchor()
            if (this.downloadObjectUrl !== null)
                window.URL.revokeObjectURL(this.downloadObjectUrl);
            this.downloadObjectUrl = window.URL.createObjectURL(blob)
            var anchor = this.downloadAnchor
            anchor.href = this.downloadObjectUrl
            anchor.download = filename
            anchor.click()
        }
    }

    private async getList(id) {
        this.data = await MassMailService.getList(id);
    }

    async processMassMail(type: String) {
        let outputModels = this.listFilters.getFormattedOutputModels();
        let params: any = {
            p: outputModels['type'],
            c: outputModels['classes'].map(c => c.id),
            a: 'all'
        }
        let blob;
        if (outputModels['email'].length == 1) {
            params.mail = outputModels['email'][0].indexOf('users.with.mail') >= 0;
        }
        if (outputModels['code'].length == 1) {
            params.a = outputModels['code'][0].indexOf('users.activated') >= 0;
        }
       try {
           blob = await this.spinner.perform('portal-content', MassMailService.massMailProcess(this.structureId, type, params));
       } catch (error) {
           this.ns.error("massmail.error", "error", error);
           return
       }
       if (type.indexOf("pdf") > -1) {
          this.ajaxDownload(blob, this.translate("massmail.filename") + ".pdf");
          this.ns.success("massmail.pdf.done");
       } else {
          this.ns.success("massmail.mail.done");
       }
    }

    deselect(filter, item) {
        filter.outputModel.splice(filter.outputModel.indexOf(item), 1)
        filter.observable.next()
        this.deselectItem = true;
    }

    setUserOrder(order: string) {
        this.userOrder = this.userOrder === order ? '-' + order : order;
    }

    getData(){
        let users = FilterPipe.prototype.transform(this.data, this.filters) || []
        this.countUsers = 0;
        this.countUsersWithoutMail = 0;
        users.forEach( user => {
            this.countUsers++;
            if (!user.email)
                this.countUsersWithoutMail++;
        })
        return users;

    }

    setFiltersOnStyle = () => {
        return { 'filtersOn': this.listFilters.filters.some(f => f.outputModel && f.outputModel.length > 0) }
    }

    onClick(event) {
        if (this.show 
                && event.target != this.filtersToggleRef.nativeElement 
                && !this.filtersToggleRef.nativeElement.contains(event.target)
                && !this.filtersDivRef.nativeElement.contains(event.target)
                && !this.deselectItem) {
            this.toggleVisibility();
        }
        this.deselectItem = false
        return true
    }

    toggleVisibility(): void {
        this.show = !this.show
    }
}

export class MailFilter extends UserFilter<string> {
    type = 'email'
    label = 'email'
    comboModel = ['users.with.mail', 'users.without.mail']

    filter = (mail: string) => {
        let outputModel = this.outputModel
        return outputModel.length === 0 ||
            outputModel.indexOf('users.without.mail') >= 0 && !mail ||
            outputModel.indexOf('users.with.mail') >= 0 && !(!mail)
    }
}
