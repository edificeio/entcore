import { NgModule, Optional, SkipSelf } from '@angular/core'
import { CommonModule } from '@angular/common'
import { RouterModule } from '@angular/router'

import { InfraComponentsModule, LabelsService } from 'infra-components'
import { SijilModule } from 'sijil'

import { UxModule } from '../shared/ux/ux.module'
import { NavComponent } from './nav/nav.component'
import { SpinnerService, SijilLabelsService, NotifyService } from './services'
import { I18nResolver } from './resolvers/i18n.resolver'
import { SessionResolver } from './resolvers/session.resolver'
import { StructuresResolver } from './resolvers/structures.resolver'
import { throwIfAlreadyLoaded } from './module-import.guard'

@NgModule({
    imports: [
        CommonModule,
        SijilModule.forRoot(),
        InfraComponentsModule.forRoot({
            provide: LabelsService,
            useExisting: SijilLabelsService
        }),
        RouterModule,
        UxModule
    ],
    exports: [NavComponent],
    declarations: [NavComponent],
    providers: [
        SessionResolver,
        StructuresResolver,
        I18nResolver,
        SijilLabelsService,
        NotifyService,
        SpinnerService
    ],
})
export class CoreModule {
    constructor(@Optional() @SkipSelf() parentModule: CoreModule) {
        throwIfAlreadyLoaded(parentModule, 'CoreModule')
    }
}