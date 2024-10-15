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
import { UserPositionService } from 'src/app/core/services/user-position.service';

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
        { label: 'management.calendar', view: 'calendar', active: 'calendar', right: "fr.openent.DisplayController|view"},
        { label: 'management.zimbra.tab', view: 'zimbra', active: 'zimbra', right: "fr.openent.zimbra.controllers.ZimbraController|view"},
        { label: 'management.subjects.tab', view: 'subjects/create', active: 'subjects', right: "fr.openent.DisplayController|view"},
        { label: 'management.edt.tab', view: 'import-edt', active: 'import-edt', right: "fr.cgi.edt.controllers.EdtController|view" },
        { label: 'management.structure.gar.tab', view: 'gar', active: 'gar'},
        { label: "management.structure.user-position.tab", view: "positions", active: "positions" },
    ];

    private structure: StructureModel;

    private displayZimbra: boolean;

    private displaySubjects: boolean;

    private displayEdt: boolean;

    private displayCalendar: boolean;

    constructor(injector: Injector, private zimbraService: ZimbraService, private subjectsService: SubjectsService,
                private calendarService: CalendarService, private importEDTReportsService: ImportEDTReportsService,
                private userPositionService: UserPositionService) {
        super(injector);
    }

    ngOnInit(): void {
        super.ngOnInit();
        this.zimbraTabEnable();
        this.subjectsTabEnable();
        this.edtTabEnable();
        this.calendarTabEnable();
        this.userPositionsTabEnable();
        this.tabEnable("gar", false);
        this.changeDetector.markForCheck();
        // Watch selected structure
        this.subscriptions.add(routing.observe(this.route, 'data').subscribe((data: Data) => {
            if (data.structure) {
                this.structure = data.structure;
                this.changeDetector.markForCheck();
            }
        }));

        this.subscriptions.add(this.router.events.subscribe(e => {
            if (e instanceof NavigationEnd) {
                this.changeDetector.markForCheck();
            }
        }));
    }

    private async tabEnable(viewName: string, authorise: boolean)
    {
        const session: Session = await SessionModel.getSession();
        for (let i = this.tabs.length; i-- > 0;) {
            if (this.tabs[i].view === viewName && !authorise) {
                if(this.tabs[i].right == null || session.hasRight(this.tabs[i].right) == false)
                {
                    if(session.isADMC() == false)
                    {
                        this.tabs.splice(i, 1);
                        this.changeDetector.markForCheck();
                    }
                }
            }
        }
    }

    subjectsTabEnable(): void {
        /* Remove subjects tab if the config key is set to false */
        this.subjectsService.getSubjectsConfKey().subscribe((conf) => {
            this.displaySubjects = conf.displaySubjects;
            this.tabEnable("subjects/create", this.displaySubjects);
        });
    }

    zimbraTabEnable(): void {
        /* Remove zimbra tab if the config key is set to false */
        this.zimbraService.getZimbraConfKey().subscribe((conf) => {
            this.displayZimbra = conf.displayZimbra;
            this.tabEnable("zimbra", this.displayZimbra);
        });
    }

    edtTabEnable(): void {
        /* Remove edt tab if the config key is set to false */
        this.importEDTReportsService.getEdtConfKey().subscribe((conf) => {
            this.displayEdt = conf.displayEdt;
            this.tabEnable("import-edt", this.displayEdt);
        });
    }

    calendarTabEnable(): void {
        /* Remove calendar tab if the config key is set to false */
        this.calendarService.getCalendarConfKey().subscribe((conf) => {
            this.displayCalendar = conf.displayCalendar;
            this.tabEnable("calendar", this.displayCalendar);
        });
    }

    userPositionsTabEnable(): void {
        /* Remove UserPosition tab if the config restricts its use */
        this.userPositionService.isTabEnabled().subscribe(enabled => {
            this.tabEnable("positions", enabled);
        });
    }

    onError(error: Error) {
        console.error(error);
    }

    isActive(path: string): boolean {
        return this.router.isActive('/admin/' + this.structure.id + '/management/' + path, false);
    }

}
