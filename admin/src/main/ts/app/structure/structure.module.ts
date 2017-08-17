import { NgModule } from '@angular/core'
import { CommonModule } from '@angular/common'
import { FormsModule } from '@angular/forms'
import { RouterModule } from '@angular/router'

import { InfraComponentsModule } from 'infra-components'
import { SijilModule } from 'sijil'

import { CoreModule } from "../core/core.module";
import { UxModule } from '../shared/ux/ux.module'
import { routes } from './structure-routing.module'
import { StructureResolve } from './structure.resolve'
import { StructureComponent } from './structure.component'
import { StructureHomeComponent } from './structure-home.component';
import { StructureCard, ImportsExportsCard, QuickActionsCard, UserSearchCard } from './cards'

@NgModule({
    imports: [
        CommonModule,
        FormsModule,
        InfraComponentsModule.forChild(),
        RouterModule.forChild(routes),
        SijilModule.forChild(),
        UxModule
    ],
    declarations: [
        StructureComponent,
        StructureHomeComponent,
        StructureCard,
        ImportsExportsCard, 
        QuickActionsCard, 
        UserSearchCard
    ],
    providers: [
        StructureResolve
    ],
    exports: [
        RouterModule
    ]
})
export class StructureModule {}
