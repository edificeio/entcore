import { NgModule, Optional, SkipSelf } from '@angular/core'
import { CommonModule } from '@angular/common'
import { RouterModule } from '@angular/router'

import { InfraComponentsModule, LabelsService } from 'infra-components'
import { SijilModule } from 'sijil'

import { UxModule } from '../shared/ux/ux.module'
import { NavComponent } from './nav/nav.component'
import { SpinnerService, SijilLabelsService, NotifyService } from './services'
import { I18nResolve } from './resolvers/i18n.resolve'
import { SessionResolve } from './resolvers/session.resolve'
import { StructuresResolve } from './resolvers/structures.resolve'
import { throwIfAlreadyLoaded } from './module-import-guard'

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
        SessionResolve,
        StructuresResolve,
        I18nResolve,
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