import { CommonModule } from '@angular/common';
import { NgModule, Optional, SkipSelf } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterModule } from '@angular/router';
import { NgxOdeSijilModule } from 'ngx-ode-sijil';
import { LabelsService, NgxOdeUiModule, SpinnerService } from 'ngx-ode-ui';
import { throwIfAlreadyLoaded } from './module-import.guard';
import { ConfigResolver } from './resolvers/config.resolver';
import { I18nResolver } from './resolvers/i18n.resolver';
import { SessionResolver } from './resolvers/session.resolver';
import { StructuresResolver } from './resolvers/structures.resolver';
import { NotifyService } from './services/notify.service';
import { SijilLabelsService } from './services/sijil.labels.service';
import { UserService } from './services/user.service';
import { WidgetService } from './services/widgets.service';
import { UserPositionServices } from './services/user-position.service';
import { FavIconResolver } from './resolvers/favicon.resolver';

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
    providers: [
        SessionResolver,
        StructuresResolver,
        I18nResolver,
        SijilLabelsService,
        NotifyService,
        SpinnerService,
        WidgetService,
        ConfigResolver,
        UserService,
        UserPositionServices,
        FavIconResolver
    ],
})
export class CoreModule {
    constructor(@Optional() @SkipSelf() parentModule: CoreModule) {
        throwIfAlreadyLoaded(parentModule, 'CoreModule');
    }
}
