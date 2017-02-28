import { NgModule, Provider } from '@angular/core'
import { BrowserModule } from '@angular/platform-browser'
import { RouterModule } from '@angular/router'
import { FormsModule } from '@angular/forms'

import { SijilModule } from 'sijil'
import { InfraComponentsModule, LabelsService } from 'infra-components/dist'
import { UxModule } from './modules'

import { AdminRoot, Portal, Home, StructureCard, StructureHome, ImportsExportsCard, QuickActionsCard, UserSearchCard } from './components'
import { SessionResolve, StructureResolve, StructuresResolve, I18nResolve } from './routing'
import { routes } from './routes'
import { LoadingService, SijilLabelsService, UserListService, UserlistFiltersService } from './services'

@NgModule({
    imports: [
        BrowserModule,
        FormsModule,
        SijilModule.forRoot(),
        InfraComponentsModule.forRoot({
            provide: LabelsService,
            useExisting: SijilLabelsService
        }),
        RouterModule.forRoot(routes),
        UxModule
    ],
    declarations: [
        AdminRoot,
        Portal,
        Home,
        StructureCard,
        StructureHome,
        ImportsExportsCard,
        QuickActionsCard,
        StructureCard,
        UserSearchCard
    ],
    providers: [
        UserListService,
        SessionResolve,
        StructureResolve,
        StructuresResolve,
        I18nResolve,
        LoadingService,
        SijilLabelsService
    ],
    bootstrap: [ AdminRoot ]
})
export class AdminModule {}
