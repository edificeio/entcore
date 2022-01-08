import { Component } from "@angular/core";
import { OdeComponent } from "ngx-ode-core";

@Component({
    selector: 'ode-admc-dashboard',
    template: `
        <h1>
            <i class="dashboard"></i><s5l>admc.home.title</s5l>
        </h1>
        <div class="card-layout"> 
            <ode-admc-communication-card></ode-admc-communication-card>
            <ode-admc-search-card></ode-admc-search-card>
            <ode-admc-apps-card></ode-admc-apps-card>
            <ode-admc-misc-card></ode-admc-misc-card>
        </div>
    `
})
export class AdmcHomeComponent extends OdeComponent {

}