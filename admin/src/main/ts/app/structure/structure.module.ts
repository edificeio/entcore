import { NgModule } from '@angular/core'
import { CommonModule } from '@angular/common'
import { FormsModule } from '@angular/forms'
import { RouterModule } from '@angular/router'

import { SijilModule } from 'sijil'

import { CoreModule } from "../core/core.module";
import { UxModule } from '../shared/ux/ux.module'
import { routes } from './structure.routing'
import { StructureResolver } from './structure.resolver'
import { StructureComponent } from './structure.component'
import { StructureHomeComponent } from './structure-home.component'
import { ServicesCard } from './cards/services-card.component'
import { ImportsExportsCard } from './cards/imports-exports-card.component'
import { QuickActionsCard } from './cards/quick-actions-card.component'
import { UserSearchCard } from './cards/user-search-card.component'

@NgModule({
    imports: [
        CommonModule,
        FormsModule,
        RouterModule.forChild(routes),
        SijilModule.forChild(),
        UxModule
    ],
    declarations: [
        StructureComponent,
        StructureHomeComponent,
        ServicesCard,
        ImportsExportsCard, 
        QuickActionsCard, 
        UserSearchCard
    ],
    providers: [
        StructureResolver
    ],
    exports: [
        RouterModule
    ]
})
export class StructureModule {}
