import { CommonModule } from "@angular/common";
import { NgModule } from "@angular/core";
import { FormsModule } from "@angular/forms";
import { RouterModule } from "@angular/router";
import { NgxOdeSijilModule } from "ngx-ode-sijil";
import { NgxOdeUiModule } from "ngx-ode-ui";
import { routes } from "./admc.routing";
import { AdmcComponent } from "./admc.component";
import { AdmcHomeComponent } from "./dashboard/admc-dashboard.component";
import { AdmcCommunicationCardComponent } from "./dashboard/cards/admc-communication-card.component";
import { AdmcSearchCardComponent } from "./dashboard/cards/admc-search-card.component";
import { AdmcAppsCardComponent } from "./dashboard/cards/admc-apps-card.component";
import { AdmcMiscCardComponent } from "./dashboard/cards/admc-misc-card.component";

@NgModule({
    imports: [
        CommonModule,
        FormsModule,
        RouterModule.forChild(routes),
        NgxOdeSijilModule.forChild(),
        NgxOdeUiModule
    ],
    declarations: [
        AdmcComponent,
        AdmcHomeComponent,
        AdmcCommunicationCardComponent,
        AdmcSearchCardComponent,
        AdmcAppsCardComponent,
        AdmcMiscCardComponent
    ],
    exports: [
        RouterModule
    ]
})
export class AdmcModule {

}