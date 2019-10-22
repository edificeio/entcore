import {
    ChangeDetectionStrategy,
    ChangeDetectorRef,
    Component,
    ElementRef,
    OnDestroy,
    OnInit,
    ViewChild
} from '@angular/core';
import { Subscription } from 'rxjs/Subscription';
import { ActivatedRoute, Data, NavigationEnd, Router } from '@angular/router';
import { routing } from '../../core/services/routing.service';
import { UserlistFiltersService } from '../../core/services/userlist.filters.service';
import { NotifyService } from '../../core/services/notify.service';
import { SpinnerService } from '../../core/services/spinner.service';
import { StructureModel, UserModel } from '../../core/store';
import { MassMailService } from './mass-mail.service';
import { BundlesService } from 'sijil';
import { FilterPipe } from '../../shared/ux/pipes';
import { SelectOption } from '../../shared/ux/components/multi-select.component';

@Component({
    selector: 'mass-mail',
    template: `
        <div class="container has-shadow">
            <h2>{{ 'massmail.accounts' | translate }}</h2>

            <div class="has-vertical-padding-10 is-pulled-left">
                <button (click)="toggleVisibility()"
                        class="button is-primary"
                        [ngClass]="setFiltersOnStyle()"
                        #filtersToggle>
                    <s5l>massmail.filters</s5l>
                    <i class="fa fa-chevron-down"></i>
                </button>

                <div [hidden]="!show" class="filters" #filtersDiv>
                    <i class="fa fa-close close" (click)="show=false"></i>

                    <div *ngFor="let filter of userlistFiltersService.filters">
                        <div *ngIf="filter.comboModel.length > 0">
                            <div>
                                <multi-combo
                                        [comboModel]="filter.comboModel"
                                        [(outputModel)]="filter.outputModel"
                                        [title]="filter.label | translate"
                                        [display]="filter.display || translate"
                                        [max]="filter.datepicker ? 1 : filter.comboModel.length"
                                        [orderBy]="filter.order || orderer"
                                        (outputModelChange)="resetDate(filter)">
                                </multi-combo>

                                <div class="multi-combo-companion">
                                    <div *ngFor="let item of filter.outputModel" (click)="deselect(filter, item)">
                                        <span *ngIf="filter.display">{{ item[filter.display] | translate }}</span>
                                        <span *ngIf="!filter.display">{{ item | translate }}</span>
                                        <i class="fa fa-trash is-size-5"></i>
                                    </div>
                                    <div *ngIf="filter.datepicker&&filter.outputModel.length>0">
                                        <date-picker [ngModel]="dateFilter"
                                                     (ngModelChange)="updateDate($event,filter)"></date-picker>
                                    </div>
                                </div>
                            </div>
                        </div>
                    </div>
                </div>
            </div>
            <div class="has-vertical-padding-10 flex has-flex-end">
                <div class="mailing__sort">
                    <p>
                        <s5l>massmail.sorttitle</s5l>
                    </p>
                    <div>
                        <span><s5l>massmail.firstsort</s5l></span>
                        <mono-select [(ngModel)]="firstSort" (ngModelChange)="updateSecondSort()"
                                     [options]="[{value: 'none', label: 'massmail.none'}, {value: 'profile', label: 'massmail.profile'}, {value: 'classname', label: 'massmail.classname'}]">
                        </mono-select>
                        <span [hidden]="firstSort === 'none'"><s5l>massmail.secondsort</s5l></span>
                        <mono-select [(ngModel)]="secondSort" [hidden]="firstSort === 'none'"
                                     [options]="secondSortOptions">
                        </mono-select>
                    </div>
                    <div class="mailing__notice">
                        <s5l>massmail.notice</s5l>
                    </div>
                </div>
                <div class="mailing__sort">
                    <p>
                        <s5l>massmail.modeltitle</s5l>
                    </p>
                    <mono-select [(ngModel)]="templateModel"
                                 [options]="[{value: 'pdf', label: 'massmail.pdf.one'}, {value: 'newPdf', label: 'massmail.pdf.two'}, {value: 'simplePdf', label: 'massmail.pdf.eight'}]">
                    </mono-select>
                </div>
                <div class="mailing__publish">
                    <p>
                        <s5l>process.massmail</s5l>
                    </p>
                    <div>
                        <button class="cell" (click)="processMassMail('pdf')" [disabled]="countUsers == 0">
                            <s5l>massmail.pdf</s5l>
                        </button>
                        <button class="cell" (click)="showConfirmation = true" [disabled]="countUsers == 0">
                            <s5l>massmail.mail</s5l>
                        </button>
                        <lightbox-confirm
                            [show]="showConfirmation"
                            [lightboxTitle]="'warning'"
                            (onConfirm)="processMassMail('mail')"
                            (onCancel)="showConfirmation = false">
                            <s5l>massmail.confirm</s5l>
                        </lightbox-confirm>
                    </div>
                </div>
            </div>

            <div class="has-vertical-padding-10 is-clearfix">
                <div class="message is-info">
                    <div class="message-body has-text-centered">{{countUsers}}
                        <s5l>massmail.users.total</s5l>
                    </div>
                </div>
                <div class="message is-warning">
                    <div class="message-body has-text-centered">{{countUsersWithoutMail}}
                        <s5l>massmail.users.nomail</s5l>
                    </div>
                </div>
            </div>

            <div class="has-vertical-padding-10">
                <table>
                    <thead>
                    <tr>
                        <th (click)="setUserOrder('lastName')"><i class="fa fa-sort"></i>
                            <s5l>lastName</s5l>
                        </th>
                        <th (click)="setUserOrder('firstName')"><i class="fa fa-sort"></i>
                            <s5l>firstName</s5l>
                        </th>
                        <th (click)="setUserOrder('type')"><i class="fa fa-sort"></i>
                            <s5l>profile</s5l>
                        </th>
                        <th (click)="setUserOrder('login')"><i class="fa fa-sort"></i>
                            <s5l>login</s5l>
                        </th>
                        <th (click)="setUserOrder('code')"><i class="fa fa-sort"></i>
                            <s5l>activation.code</s5l>
                        </th>
                        <th (click)="setUserOrder('email')"><i class="fa fa-sort"></i>
                            <s5l>email</s5l>
                        </th>
                        <th (click)="setUserOrder('classesStr')"><i class="fa fa-sort"></i>
                            <s5l>create.user.classe</s5l>
                        </th>
                        <th (click)="setUserOrder('creationDate')"><i class="fa fa-sort"></i>
                            <s5l>creation.date</s5l>
                        </th>
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
                        [routerLink]="'/admin/'+structureId+'/users/'+user.id + '/details'"
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
                        <td>{{displayDate(user.creationDate)}}</td>
                    </tr>
                    </tbody>
                </table>
            </div>
        </div>`,
    host: {
        '(document:click)': 'onClick($event)',
    },
    styles: [
        '.flex {display: flex;}',
        '.mailing__sort, .mailing__publish {margin: 0 10px;}',
        '.mailing__sort p, .mailing__publish p {margin: 0 0 5px 0;}',
        '.mailing__notice {font-style: italic; font-size: 12px; margin-top: 5px;}'
    ],
    changeDetection: ChangeDetectionStrategy.OnPush


})
export class MassMailComponent implements OnInit, OnDestroy {

    @ViewChild('filtersDiv') filtersDivRef: ElementRef;
    @ViewChild('filtersToggle') filtersToggleRef;

    users: UserModel[];
    filters: Object;
    inputFilters = {lastName: '', firstName: '', classesStr: ''};
    countUsers = 0;
    countUsersWithoutMail = 0;
    userOrder: string;
    structureId: string;
    show: boolean = false;
    private deselectItem: boolean = false;
    dateFilter: string;
    dateFormat: Intl.DateTimeFormat
    showConfirmation: boolean = false;

    dataSubscriber: Subscription
    routerSubscriber: Subscription

    downloadAnchor = null;
    downloadObjectUrl = null;

    public firstSort: "none" | "profile" | "classname" = 'none';
    public secondSort: "none" | "profile" | "classname" = 'none';
    public templateModel: "pdf" | "simplePdf" | "newPdf" = 'pdf';
    public secondSortOptions: SelectOption<string>[] = [{value: 'none', label: 'massmail.none'}];

    translate = (...args) => {
        return (<any>this.bundles.translate)(...args)
    }

    constructor(
        public route: ActivatedRoute,
        public router: Router,
        public userlistFiltersService: UserlistFiltersService,
        public cdRef: ChangeDetectorRef,
        public bundles: BundlesService,
        private ns: NotifyService,
        private spinner: SpinnerService
    ) {
    }

    ngOnInit(): void {
        this.dataSubscriber = routing.observe(this.route, "data").subscribe(async (data: Data) => {
            if (data['structure']) {
                let structure: StructureModel = data['structure']
                this.spinner.perform('portal-content', MassMailService.getUsers(structure._id)
                    .then((data) => {
                        this.users = data;
                        this.structureId = structure._id;
                        this.dateFormat = Intl.DateTimeFormat(this.bundles.currentLanguage);
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
        this.showConfirmation = false;
        let outputModels = this.userlistFiltersService.getFormattedOutputModels();
        let sorts = null;
        if (this.firstSort !== 'none') {
            sorts = [this.firstSort];
            if (this.secondSort !== 'none') {
                sorts.push(this.secondSort);
            }
        }
        if (type === 'pdf') {
            type = this.templateModel;
        }

        let params: any = {
            p: outputModels['type'],
            c: outputModels['classes'].map(c => c.id),
            a: 'all'
        };

        if (sorts) {
            params.s = sorts;
        }

        let blob;
        if (outputModels['email'].length == 1) {
            params.mail = outputModels['email'][0].indexOf('users.with.mail') >= 0;
        }
        if (outputModels['code'].length == 1) {
            params.a = outputModels['code'][0].indexOf('users.activated') >= 0;
        }
        if (outputModels['creationDate'].length == 1) {
            params.dateFilter = outputModels['creationDate'][0].comparison === 'users.before' ? 'before' : 'after';
            params.date = outputModels['creationDate'][0].date.getTime();
        }
        if (outputModels['functions'].length == 1) {
            params.adml = outputModels['functions'][0].indexOf('users.adml') >= 0;
        }

        try {
            blob = await this.spinner.perform('portal-content', MassMailService.massMailProcess(this.structureId, type, params));
        } catch (error) {
            this.ns.error("massmail.error", "error", error);
            return
        }

        if (type.toLowerCase().includes("pdf")) {
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
        this.resetDate(filter);
    }

    updateDate(newDate, filter): void {
        this.dateFilter = newDate;
        filter.outputModel[0].date = new Date(Date.parse(this.dateFilter));
    }

    displayDate(date: string): string {
        return this.dateFormat.format(new Date(date))
    }

    resetDate(filter) {
        if (filter.datepicker) {
            this.dateFilter = "";
            if (filter.outputModel.length > 0) {
                filter.outputModel[0].date = undefined;
            }
        }
    }

    setUserOrder(order: string): void {
        this.userOrder = this.userOrder === order ? '-' + order : order;
    }

    setFiltersOnStyle = () => {
        return {'is-active': this.userlistFiltersService.filters.some(f => f.outputModel && f.outputModel.length > 0)}
    }

    onClick(event) {
        if (this.show
            && event.target != this.filtersToggleRef.nativeElement
            && !this.filtersToggleRef.nativeElement.contains(event.target)
            && !this.filtersDivRef.nativeElement.contains(event.target)
            && !this.deselectItem
            && !event.target.className.toString().startsWith('flatpickr')
            && !event.target.parentElement.className.toString().startsWith('flatpickr')) {
            this.toggleVisibility();
        }
        this.deselectItem = false
        return true
    }

    toggleVisibility(): void {
        this.show = !this.show
    }

    updateSecondSort(): void {
        this.secondSort = 'none';
        this.secondSortOptions = [{value: 'none', label: 'massmail.none'}];
        if (this.firstSort === 'classname') {
            this.secondSortOptions.push({value: 'profile', label: 'massmail.profile'});
        }
        if (this.firstSort === 'profile') {
            this.secondSortOptions.push({value: 'classname', label: 'massmail.classname'});
        }
    }
}
