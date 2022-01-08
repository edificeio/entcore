import { CommonModule } from "@angular/common";
import { NgModule } from "@angular/core";
import { FormsModule } from "@angular/forms";
import { RouterModule } from "@angular/router";
import { NgxOdeSijilModule } from "ngx-ode-sijil";
import { NgxOdeUiModule } from "ngx-ode-ui";
import { AdmcAppsComponent } from "./admc-apps.component";
import { routes } from "./admc-apps.routing";
import { AdmcAppsRolesComponent } from "./roles/admc-apps-roles.component";

@NgModule({
    imports: [
        CommonModule,
        FormsModule,
        RouterModule.forChild(routes),
        NgxOdeSijilModule.forChild(),
        NgxOdeUiModule
    ],
    declarations: [
        AdmcAppsComponent,
        AdmcAppsRolesComponent
    ],
    exports: [
    ]
})
export class AdmcAppsModule {

}