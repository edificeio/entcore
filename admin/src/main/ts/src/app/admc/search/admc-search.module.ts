import { CommonModule } from "@angular/common";
import { NgModule } from "@angular/core";
import { FormsModule } from "@angular/forms";
import { RouterModule } from "@angular/router";
import { NgxOdeSijilModule } from "ngx-ode-sijil";
import { NgxOdeUiModule } from "ngx-ode-ui";
import { UserListService } from "src/app/core/services/userlist.service";
import { AdmcSearchComponent } from "./admc-search.component";
import { routes } from "./admc-search.routing";
import { AdmcSearchService } from "./admc-search.service";
import { AdmcSearchTransverseComponent } from "./transverse/admc-search-transverse.component";
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
        AdmcSearchTransverseComponent
    ],
    exports: [
    ],
    providers: [
        UserListService,
        AdmcSearchService,
        UsersStore
    ]
})
export class AdmcSearchModule {

}