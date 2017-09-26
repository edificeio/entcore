import { NgModule } from "@angular/core";
import { CommonModule, NgSwitch } from '@angular/common'
import { FormsModule } from '@angular/forms'
import { RouterModule } from '@angular/router'
import { InfraComponentsModule } from 'infra-components'
import { SijilModule } from 'sijil'

import { UxModule } from '../shared/ux/ux.module'
import { routes } from './services.routing'

import { ServicesStore } from './services.store'
import { ApplicationsResolver } from './applications/applications.resolver'
import { RolesResolver } from './applications/details/roles.resolver'

import { ServicesComponent } from "./services.component"
import { ApplicationsListComponent } from './applications/list/applications-list.component'
import { ApplicationDetailsComponent } from './applications/details/application-details.component'

@NgModule({
    imports: [
        CommonModule,
        FormsModule,
        UxModule,
        SijilModule.forChild(),
        InfraComponentsModule.forChild(),
        RouterModule.forChild(routes)
    ],
    declarations: [
        ServicesComponent,
        ApplicationsListComponent,
        ApplicationDetailsComponent
    ],
    providers: [ 
        NgSwitch,
        ServicesStore,
        ApplicationsResolver,
        RolesResolver     
    ]
})
export class ServicesModule { }