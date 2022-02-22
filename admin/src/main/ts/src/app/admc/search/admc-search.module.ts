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

@NgModule({
    imports: [
        CommonModule,
        FormsModule,
        RouterModule.forChild(routes),
        NgxOdeSijilModule.forChild(),
        NgxOdeUiModule
    ],
    declarations: [
        AdmcSearchComponent,
        AdmcSearchTransverseComponent
    ],
    exports: [
    ],
    providers: [
        UserListService,
        AdmcSearchService
    ]
})
export class AdmcSearchModule {

}