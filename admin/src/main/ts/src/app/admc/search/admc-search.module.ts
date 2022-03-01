import { CommonModule } from "@angular/common";
import { NgModule } from "@angular/core";
import { FormsModule } from "@angular/forms";
import { RouterModule } from "@angular/router";
import { NgxOdeSijilModule } from "ngx-ode-sijil";
import { NgxOdeUiModule } from "ngx-ode-ui";
import { UserListService } from "src/app/core/services/userlist.service";
import { ConfigResolver } from "src/app/core/resolvers/config.resolver";
import { AdmcSearchComponent } from "./admc-search.component";
import { routes } from "./admc-search.routing";
import { AdmcSearchService } from "./admc-search.service";
import { AdmcSearchTransverseComponent } from "./transverse/admc-search-transverse.component";
import { AdmcSearchUnlinkedComponent } from "./unlinked/admc-search-unlinked.component";
import { UnlinkedUserDetailsComponent } from "./unlinked/details/user-details.component";
import { UserDetailsResolver } from "./unlinked/details/user-details.resolver";
import { UnlinkedUserService } from "./unlinked/unlinked.service";
import { UnlinkedUserStructuresSectionComponent } from "./unlinked/structures-section/user-structures-section.component";
import { UsersModule } from "src/app/users/users.module";
import { UsersStore } from "src/app/users/users.store";

@NgModule({
    imports: [
        CommonModule,
        FormsModule,
        RouterModule.forChild(routes),
        NgxOdeSijilModule.forChild(),
        NgxOdeUiModule,
        UsersModule
    ],
    declarations: [
        AdmcSearchComponent,
        AdmcSearchTransverseComponent,
        AdmcSearchUnlinkedComponent,
        UnlinkedUserDetailsComponent,
        UnlinkedUserStructuresSectionComponent
    ],
    providers: [
        UserListService,
        AdmcSearchService,
        UsersStore,
        UserDetailsResolver,
        ConfigResolver,
        UnlinkedUserService,
        UsersStore
    ],
    exports: [
    ]
})
export class AdmcSearchModule {

}