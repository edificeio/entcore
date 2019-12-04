import { Logger } from './ode/Logger';
import { OdeComponent } from './ode/OdeComponent';
import {NgModule, Optional, SkipSelf} from '@angular/core';
import {CommonModule} from '@angular/common';
import {RouterModule} from '@angular/router';
import {NgxOdeUiModule} from 'ngx-ode-ui';
import {NgxOdeSijilModule} from 'ngx-ode-sijil';

import {NavComponent} from './nav/nav.component';
import {I18nResolver} from './resolvers/i18n.resolver';
import {SessionResolver} from './resolvers/session.resolver';
import {StructuresResolver} from './resolvers/structures.resolver';
import {throwIfAlreadyLoaded} from './module-import.guard';
import {ConfigResolver} from './resolvers/config.resolver';
import { SijilLabelsService } from './services/sijil.labels.service';
import { NotifyService } from './services/notify.service';
import {LabelsService} from 'ngx-ode-ui';
import { GroupPickerComponent } from './components/group-picker/group-picker.component';
import { FormsModule } from '@angular/forms';

@NgModule({
    imports: [
        CommonModule,
        FormsModule,
        NgxOdeSijilModule.forRoot(),
        NgxOdeUiModule.forRoot({
            provide: LabelsService,
            useExisting: SijilLabelsService
        }),
        RouterModule
    ],
    exports: [NavComponent, OdeComponent, GroupPickerComponent],
    declarations: [NavComponent, OdeComponent, GroupPickerComponent],
    providers: [
        SessionResolver,
        StructuresResolver,
        I18nResolver,
        SijilLabelsService,
        NotifyService,
        ConfigResolver,
        Logger
    ],
})
export class CoreModule {
    constructor(@Optional() @SkipSelf() parentModule: CoreModule) {
        throwIfAlreadyLoaded(parentModule, 'CoreModule');
    }
}
