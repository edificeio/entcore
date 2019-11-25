import {NgModule} from '@angular/core';
import {CommonModule} from '@angular/common';
import {FormsModule} from '@angular/forms';
import {RouterModule} from '@angular/router';

import {SijilModule} from 'sijil';
import {UxModule} from '../shared/ux/ux.module';
import {routes} from './structure.routing';
import {StructureResolver} from './structure.resolver';
import {StructureComponent} from './structure.component';
import {StructureHomeComponent} from './structure-home/structure-home.component';
import {ServicesCardComponent} from './cards/services-card/services-card.component';
import {ImportsExportsCardComponent} from './cards/imports-exports-card/imports-exports-card.component';
import {QuickActionsCardComponent} from './cards/quick-actions-card/quick-actions-card.component';
import {UserSearchCardComponent} from './cards/user-search-card/user-search-card.component';

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
        ServicesCardComponent,
        ImportsExportsCardComponent,
        QuickActionsCardComponent,
        UserSearchCardComponent
    ],
    providers: [
        StructureResolver
    ],
    exports: [
        RouterModule
    ]
})
export class StructureModule {

}
