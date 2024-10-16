import { CommonModule } from "@angular/common";
import { NgModule } from "@angular/core";
import { FormsModule } from "@angular/forms";
import { RouterModule } from "@angular/router";
import { NgxOdeSijilModule } from "ngx-ode-sijil";
import { NgxOdeUiModule } from "ngx-ode-ui";
import { AdmcMiscComponent } from "./admc-misc.component";
import { GarComponent } from "./gar/gar.component";
import { routes } from "./admc-misc.routing";

@NgModule({
    imports: [
        CommonModule,
        FormsModule,
        RouterModule.forChild(routes),
        NgxOdeSijilModule.forChild(),
        NgxOdeUiModule
    ],
    declarations: [
        AdmcMiscComponent,
        GarComponent
    ],
    exports: [
    ]
})
export class AdmcMiscModule {

}