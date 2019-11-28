import {ChangeDetectionStrategy, ChangeDetectorRef, Component, ElementRef, OnDestroy, OnInit, ViewChild} from '@angular/core';
import {Subscription} from 'rxjs';
import {ActivatedRoute, Data, NavigationEnd, Router} from '@angular/router';
import {routing} from '../../../core/services/routing.service';
import {UserlistFiltersService} from '../../../core/services/userlist.filters.service';
import {NotifyService} from '../../../core/services/notify.service';
import {SpinnerService} from '../../../core/services/spinner.service';
import {MassMailService} from '../mass-mail.service';
import {BundlesService} from 'sijil';
import {FilterPipe} from '../../../shared/ux/pipes';
import {SelectOption} from '../../../shared/ux/components/multi-select/multi-select.component';
import { UserModel } from 'src/app/core/store/models/user.model';
import { StructureModel } from 'src/app/core/store/models/structure.model';

@Component({
    selector: 'ode-mass-mail',
    templateUrl: './mass-mail.component.html',
    host: {
        '(document:click)': 'onClick($event)',
    },
    styleUrls: ['./mass-mail.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush


})
export class MassMailComponent implements OnInit, OnDestroy {

    @ViewChild('filtersDiv', {static: false}) filtersDivRef: ElementRef;
    @ViewChild('filtersToggle', {static: false}) filtersToggleRef;
    users: UserModel[];
    filters: {};
    inputFilters = {lastName: '', firstName: '', classesStr: ''};
    countUsers = 0;
    countUsersWithoutMail = 0;
    userOrder: string;
    structureId: string;
    show = false;
    private deselectItem = false;
    dateFilter: string;
    dateFormat: Intl.DateTimeFormat;
    showConfirmation = false;

    dataSubscriber: Subscription;
    routerSubscriber: Subscription;

    downloadAnchor = null;
    downloadObjectUrl = null;

    public firstSort: 'none' | 'profile' | 'classname' = 'none';
    public secondSort: 'none' | 'profile' | 'classname' = 'none';
    public templateModel: 'pdf' | 'simplePdf' | 'newPdf' = 'pdf';
    public secondSortOptions: SelectOption<string>[] = [{value: 'none', label: 'massmail.none'}];

    translate = (...args) => {
        return (this.bundles.translate as any)(...args);
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
        this.dataSubscriber = routing.observe(this.route, 'data').subscribe(async (data: Data) => {
            if (data.structure) {
                const structure: StructureModel = data.structure;
                this.spinner.perform('portal-content', MassMailService.getUsers(structure._id)
                    .then((data) => {
                        this.users = data;
                        this.structureId = structure._id;
                        this.dateFormat = Intl.DateTimeFormat(this.bundles.currentLanguage);
                        this.initFilters(structure);
                        this.filters = this.userlistFiltersService.getFormattedFilters();
                        this.cdRef.detectChanges();
                    }).catch(err => {
                        this.ns.error('massmail.error', 'error', err);
                    })
                );
            }
        });

        this.routerSubscriber = this.router.events.subscribe(e => {
            if (e instanceof NavigationEnd) {
                this.cdRef.markForCheck();
            }
        });
    }

    ngOnDestroy(): void {
        this.dataSubscriber.unsubscribe();
        this.routerSubscriber.unsubscribe();
    }

    private initFilters(structure: StructureModel): void {
        this.userlistFiltersService.resetFilters();
        this.userlistFiltersService.setDuplicatesComboModel([]);
        this.userlistFiltersService.setClassesComboModel(structure.classes);
        this.userlistFiltersService.setProfilesComboModel(structure.profiles.map(p => p.name));
    }

    getFilteredUsers(): UserModel[] {
        const users = FilterPipe.prototype.transform(this.users, this.filters) || [];
        this.countUsers = 0;
        this.countUsersWithoutMail = 0;
        users.forEach(user => {
            this.countUsers++;
            if (!user.email) {
                this.countUsersWithoutMail++;
            }
        });
        return users;
    }

    async processMassMail(type: string): Promise<void> {
        this.showConfirmation = false;
        const outputModels = this.userlistFiltersService.getFormattedOutputModels();
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

        const params: any = {
            p: outputModels.type,
            c: outputModels.classes.map(c => c.id),
            a: 'all'
        };

        if (sorts) {
            params.s = sorts;
        }

        let blob;
        if (outputModels.email.length == 1) {
            params.mail = outputModels.email[0].indexOf('users.with.mail') >= 0;
        }
        if (outputModels.code.length == 1) {
            params.a = outputModels.code[0].indexOf('users.activated') >= 0;
        }
        if (outputModels.creationDate.length == 1) {
            params.dateFilter = outputModels.creationDate[0].comparison === 'users.before' ? 'before' : 'after';
            params.date = outputModels.creationDate[0].date.getTime();
        }
        if (outputModels.functions.length == 1) {
            params.adml = outputModels.functions[0].indexOf('users.adml') >= 0;
        }

        try {
            blob = await this.spinner.perform('portal-content', MassMailService.massMailProcess(this.structureId, type, params));
        } catch (error) {
            this.ns.error('massmail.error', 'error', error);
            return;
        }

        if (type.toLowerCase().includes('pdf')) {
            this.ajaxDownload(blob, this.translate('massmail.filename') + '.pdf');
            this.ns.success('massmail.pdf.done');
        } else {
            this.ns.success('massmail.mail.done');
        }
    }

    private createDownloadAnchor(): void {
        this.downloadAnchor = document.createElement('a');
        this.downloadAnchor.style = 'display: none';
        document.body.appendChild(this.downloadAnchor);
    }

    private ajaxDownload(blob, filename): void {
        if (window.navigator.msSaveOrOpenBlob) {
            // IE specific
            window.navigator.msSaveOrOpenBlob(blob, filename);
        } else {
            // Other browsers
            if (this.downloadAnchor === null) {
                this.createDownloadAnchor();
            }
            if (this.downloadObjectUrl !== null) {
                window.URL.revokeObjectURL(this.downloadObjectUrl);
            }
            this.downloadObjectUrl = window.URL.createObjectURL(blob);
            const anchor = this.downloadAnchor;
            anchor.href = this.downloadObjectUrl;
            anchor.download = filename;
            anchor.click();
        }
    }

    deselect(filter, item): void {
        filter.outputModel.splice(filter.outputModel.indexOf(item), 1);
        filter.observable.next();
        this.deselectItem = true;
        this.resetDate(filter);
    }

    updateDate(newDate, filter): void {
        this.dateFilter = newDate;
        filter.outputModel[0].date = new Date(Date.parse(this.dateFilter));
    }

    displayDate(date: string): string {
        return this.dateFormat.format(new Date(date));
    }

    resetDate(filter) {
        if (filter.datepicker) {
            this.dateFilter = '';
            if (filter.outputModel.length > 0) {
                filter.outputModel[0].date = undefined;
            }
        }
    }

    setUserOrder(order: string): void {
        this.userOrder = this.userOrder === order ? '-' + order : order;
    }

    setFiltersOnStyle = () => {
        return {'is-active': this.userlistFiltersService.filters.some(f => f.outputModel && f.outputModel.length > 0)};
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
        this.deselectItem = false;
        return true;
    }

    toggleVisibility(): void {
        this.show = !this.show;
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
