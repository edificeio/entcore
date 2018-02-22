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
                <button (click)="toggleVisibility()" 
                    class="button" 
                    [ngClass]="setFiltersOnStyle()" 
                    #filtersToggle>
                    <s5l>massmail.filters</s5l> 
                    <i class="fa fa-filter filters-toggle"></i>
                </button>
                
                <div [hidden]="!show" class="filters" #filtersDiv>
                    <i class="fa fa-close close" (click)="show=false"></i>

                    <div *ngFor="let filter of userlistFiltersService.filters">
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
                                <input class="twelve" type="text" [(ngModel)]="inputFilters.lastName" 
                                    [attr.placeholder]="'search' | translate"/>
                            </th>
                            <th>
                                <input type="text" [(ngModel)]="inputFilters.firstName" 
                                    [attr.placeholder]="'search' | translate"/>
                            </th>
                            <th colspan="4"></th>
                            <th>
                                <input type="text" [(ngModel)]="inputFilters.classesStr" 
                                    [attr.placeholder]="'search' | translate"/>
                            </th>
                        </tr>
                    </thead>
                    <tbody>
                        <tr *ngFor="let user of (getFilteredUsers() | filter: inputFilters) | orderBy: userOrder "
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

    @ViewChild('filtersDiv') filtersDivRef: ElementRef;
    @ViewChild('filtersToggle') filtersToggleRef;

    users: UserModel[];
    filters: Object;
    inputFilters = { lastName: '', firstName: '', classesStr: '' };
    countUsers = 0;
    countUsersWithoutMail = 0;
    userOrder: string;
    structureId: string;
    show: boolean = false;
    private deselectItem: boolean = false;

    dataSubscriber: Subscription
    routerSubscriber: Subscription

    downloadAnchor = null;
    downloadObjectUrl = null;

    translate = (...args) => { return (<any>this.bundles.translate)(...args) }

    constructor(
        public route: ActivatedRoute,
        public router: Router,
        public userlistFiltersService: UserlistFiltersService,
        public cdRef: ChangeDetectorRef,
        public bundles: BundlesService,
        private ns: NotifyService,
        private spinner: SpinnerService
    ) { }

    ngOnInit(): void {
        this.dataSubscriber = routing.observe(this.route, "data").subscribe(async (data: Data) => {
            if (data['structure']) {
                let structure: StructureModel = data['structure']
                this.spinner.perform('portal-content', MassMailService.getUsers(structure._id)
                    .then((data) => {
                        this.users = data;
                        this.structureId = structure._id;
                        this.initFilters(structure)
                        this.filters = this.userlistFiltersService.getFormattedFilters();
                        this.cdRef.detectChanges();
                    }).catch(err => {
                        this.ns.error("massmail.error", "error", err);
                    })
                );
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

    private initFilters(structure: StructureModel): void {
        this.userlistFiltersService.resetFilters();
        this.userlistFiltersService.setDuplicatesComboModel([]);
        this.userlistFiltersService.setClassesComboModel(structure.classes);
        this.userlistFiltersService.setProfilesComboModel(structure.profiles.map(p => p.name));
    }

    getFilteredUsers(): UserModel[] {
        let users = FilterPipe.prototype.transform(this.users, this.filters) || []
        this.countUsers = 0;
        this.countUsersWithoutMail = 0;
        users.forEach(user => {
            this.countUsers++;
            if (!user.email)
                this.countUsersWithoutMail++;
        })
        return users;

    }

    async processMassMail(type: String): Promise<void> {
        let outputModels = this.userlistFiltersService.getFormattedOutputModels();

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

    private createDownloadAnchor(): void {
        this.downloadAnchor = document.createElement('a');
        this.downloadAnchor.style = "display: none";
        document.body.appendChild(this.downloadAnchor);
    }

    private ajaxDownload(blob, filename): void {
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

    deselect(filter, item): void {
        filter.outputModel.splice(filter.outputModel.indexOf(item), 1)
        filter.observable.next()
        this.deselectItem = true;
    }

    setUserOrder(order: string): void {
        this.userOrder = this.userOrder === order ? '-' + order : order;
    }

    setFiltersOnStyle = () => {
        return { 'is-active': this.userlistFiltersService.filters.some(f => f.outputModel && f.outputModel.length > 0) }
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
