import { ChangeDetectionStrategy, Component, Injector, OnDestroy, OnInit } from '@angular/core';
import { Data, NavigationEnd } from '@angular/router';
import { OdeComponent } from 'ngx-ode-core';
import { routing } from '../../core/services/routing.service';
import { StructureModel } from '../../core/store/models/structure.model';
import {ZimbraService} from '../zimbra/zimbra.service';
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
        { label: 'management.message.flash', view: 'message-flash/list', active: 'message-flash'},
        { label: 'management.block.profile.tab', view: 'block-profiles', active: 'block-profiles'},
        { label: 'management.calendar', view: 'calendar', active: 'calendar'},
        { label: 'management.zimbra.tab', view: 'zimbra', active: 'zimbra'},
        { label: 'management.edt.tab', view: 'import-edt', active: 'import-edt'},
        { label: 'create.subjects', view: 'subjects/create', active: 'subjects'}
    ];

    edt_tab = { label: 'management.edt.tab', view: 'import-edt', active: 'import-edt'};

    private structure: StructureModel;

    private displayZimbra : boolean;

    constructor(injector: Injector, private zimbraService: ZimbraService) {
        super(injector);
    }

    async admcSpecific() {
        const session: Session = await SessionModel.getSession();
        if(session.isADMC() == true || this.structure.adminEDT == true)
        {
            for(let i = this.tabs.length; i-- > 0;)
                if(this.tabs[i] == this.edt_tab)
                    return;
            this.tabs.push(this.edt_tab);
        }
        else
        {
            for(let i = this.tabs.length; i-- > 0;)
                if(this.tabs[i] == this.edt_tab)
                    this.tabs.splice(i, 1);
        }
    }

    ngOnInit(): void {
        super.ngOnInit();
        // Watch selected structure
        this.subscriptions.add(routing.observe(this.route, 'data').subscribe((data: Data) => {
            if (data.structure) {
                /* Remove zimbra tab if the config key is set to false */
                this.zimbraService.getZimbraConfKey().subscribe((conf) => {
                    this.displayZimbra = conf.displayZimbra;
                    for (let i = 0; i < this.tabs.length; i++) {
                        if (this.tabs[i].view === 'zimbra' && !this.displayZimbra) {
                            this.tabs.splice(i, 1);
                        }
                    }
                });
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

    onError(error: Error) {
        console.error(error);
    }

    isActive(path: string): boolean {
        return this.router.isActive('/admin/' + this.structure.id + '/management/' + path, false);
    }

}
