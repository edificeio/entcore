import { NgModule } from '@angular/core'
import { BrowserModule } from '@angular/platform-browser'
import { RouterModule } from '@angular/router'
import { FormsModule } from '@angular/forms'

import { SijilModule } from 'sijil'
import { InfraComponentsModule, LabelsService } from 'infra-components/dist'
import { routes } from './routing'
import { SijilLabelsService } from './services'
import { AdminRoot } from './components'

import { declarations, providers } from './module.properties'

@NgModule({
    imports: [
        BrowserModule,
        FormsModule,
        SijilModule,
        InfraComponentsModule.forRoot({
            'LabelsService': {
                provide: LabelsService,
                useExisting: SijilLabelsService
            }
        }),
        RouterModule.forRoot(routes)
    ],
    declarations: [
        ...declarations
    ],
    providers: [
        ...providers
    ],
    bootstrap: [ AdminRoot ]
})
export class AdminModule {}
