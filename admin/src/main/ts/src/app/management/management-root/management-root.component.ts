import { ChangeDetectionStrategy, Component, Injector, OnDestroy, OnInit } from '@angular/core';
import { Data, NavigationEnd } from '@angular/router';
import { OdeComponent } from 'ngx-ode-core';
import { routing } from '../../core/services/routing.service';
import { StructureModel } from '../../core/store/models/structure.model';
import {ZimbraService} from '../zimbra/zimbra.service';
import {SubjectsService} from '../subjects/subjects.service';
import {CalendarService} from '../calendar/calendar.service';
import {ImportEDTReportsService} from '../import-edt/import-edt-reports.service';
import { Session } from 'src/app/core/store/mappings/session';
import { SessionModel } from 'src/app/core/store/models/session.model';

@Component({
    selector: 'ode-management-root',
    templateUrl: './management-root.component.html',
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class ManagementRootComponent extends OdeComponent implements OnInit, OnDestroy {

    // Tabs
    tabs = [
        { label: 'management.structure.informations.tab', view: 'infos', active: 'infos'},
        { label: 'management.message.flash', view: 'message-flash/list', active: 'message-flash'},
        { label: 'management.block.profile.tab', view: 'block-profiles', active: 'block-profiles'},
        { label: 'management.calendar', view: 'calendar', active: 'calendar'},
        { label: 'management.zimbra.tab', view: 'zimbra', active: 'zimbra'},
        { label: 'management.subjects.tab', view: 'subjects/create', active: 'subjects'}
    ];

    edtTab = { label: 'management.edt.tab', view: 'import-edt', active: 'import-edt'};

    private structure: StructureModel;

    private displayZimbra: boolean;

    private displaySubjects: boolean;

    private displayEdt: boolean;

    private displayCalendar: boolean;

    constructor(injector: Injector, private zimbraService: ZimbraService, private subjectsService: SubjectsService,
                private calendarService: CalendarService, private importEDTReportsService: ImportEDTReportsService) {
        super(injector);
    }

    async admcSpecific() {
        const session: Session = await SessionModel.getSession();
        if (session.isADMC() || (this.structure != null && this.structure.adminEDT) || this.displayEdt) {
            for (let i = this.tabs.length; i-- > 0;) {
                if (this.tabs[i] === this.edtTab) {
                    return;
                }
            }
            this.tabs.push(this.edtTab);
        } else {
            for (let i = this.tabs.length; i-- > 0;) {
                if (this.tabs[i] === this.edtTab) {
                    this.tabs.splice(i, 1);
                }
            }
        }
        this.changeDetector.markForCheck();
    }

    ngOnInit(): void {
        super.ngOnInit();
        this.zimbraTabEnable();
        this.subjectsTabEnable();
        this.edtTabEnable();
        this.calendarTabEnable();
        this.changeDetector.markForCheck();
        // Watch selected structure
        this.subscriptions.add(routing.observe(this.route, 'data').subscribe((data: Data) => {
            if (data.structure) {
                this.structure = data.structure;
                this.admcSpecific();
                this.changeDetector.markForCheck();
            }
        }));

        this.subscriptions.add(this.router.events.subscribe(e => {
            if (e instanceof NavigationEnd) {
                this.changeDetector.markForCheck();
            }
        }));
    }

    subjectsTabEnable(): void {
        /* Remove subjects tab if the config key is set to false */
        this.subjectsService.getSubjectsConfKey().subscribe((conf) => {
            this.displaySubjects = conf.displaySubjects;
            for (let i = this.tabs.length; i-- > 0;) {
                if (this.tabs[i].view === 'subjects/create' && !this.displaySubjects) {
                    this.tabs.splice(i, 1);
                    this.changeDetector.markForCheck();
                }
            }
        });
    }

    zimbraTabEnable(): void {
        /* Remove zimbra tab if the config key is set to false */
        this.zimbraService.getZimbraConfKey().subscribe((conf) => {
            this.displayZimbra = conf.displayZimbra;
            for (let i = this.tabs.length; i-- > 0;) {
                if (this.tabs[i].view === 'zimbra' && !this.displayZimbra) {
                    this.tabs.splice(i, 1);
                    this.changeDetector.markForCheck();
                }
            }
        });
    }

    edtTabEnable(): void {
        /* Remove edt tab if the config key is set to false */
        this.importEDTReportsService.getEdtConfKey().subscribe((conf) => {
            this.displayEdt = conf.displayEdt;
            for (let i = this.tabs.length; i-- > 0;) {
                if (this.tabs[i].view === 'import-edt' && !this.displayEdt) {
                    this.tabs.splice(i, 1);
                    this.changeDetector.markForCheck();
                }
            }
            this.admcSpecific();
        });
    }

    calendarTabEnable(): void {
        /* Remove calendar tab if the config key is set to false */
        this.calendarService.getCalendarConfKey().subscribe((conf) => {
            this.displayCalendar = conf.displayCalendar;
            for (let i = this.tabs.length; i-- > 0;) {
                if (this.tabs[i].view === 'calendar' && !this.displayCalendar) {
                    this.tabs.splice(i, 1);
                    this.changeDetector.markForCheck();
                }
            }
        });
    }

    onError(error: Error) {
        console.error(error);
    }

    isActive(path: string): boolean {
        return this.router.isActive('/admin/' + this.structure.id + '/management/' + path, false);
    }

}
