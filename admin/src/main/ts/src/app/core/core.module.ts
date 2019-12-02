import { Logger } from './ode/Logger';
import { OdeComponent } from './ode/OdeComponent';
import {NgModule, Optional, SkipSelf} from '@angular/core';
import {CommonModule} from '@angular/common';
import {RouterModule} from '@angular/router';

import {SijilModule} from 'sijil';

import {UxModule} from '../shared/ux/ux.module';
import {LabelsService} from '../shared/ux/services';

import {NavComponent} from './nav/nav.component';
import {I18nResolver} from './resolvers/i18n.resolver';
import {SessionResolver} from './resolvers/session.resolver';
import {StructuresResolver} from './resolvers/structures.resolver';
import {throwIfAlreadyLoaded} from './module-import.guard';
import {ConfigResolver} from './resolvers/config.resolver';
import { SijilLabelsService } from './services/sijil.labels.service';
import { NotifyService } from './services/notify.service';
import { SpinnerService } from './services/spinner.service';

@NgModule({
    imports: [
        CommonModule,
        SijilModule.forRoot(),
        UxModule.forRoot({
            provide: LabelsService,
            useExisting: SijilLabelsService
        }),
        RouterModule
    ],
    exports: [NavComponent, OdeComponent],
    declarations: [NavComponent, OdeComponent],
    providers: [
        SessionResolver,
        StructuresResolver,
        I18nResolver,
        SijilLabelsService,
        NotifyService,
        SpinnerService,
        ConfigResolver,
        Logger
    ],
})
export class CoreModule {
    constructor(@Optional() @SkipSelf() parentModule: CoreModule) {
        throwIfAlreadyLoaded(parentModule, 'CoreModule');
    }
}
