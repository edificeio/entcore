import { Component } from "@angular/core";
import { OdeComponent } from "ngx-ode-core";

@Component({
    selector: 'ode-admc-apps-card',
    template: `
    <div class="card-header">
        <span>
            <s5l>admc.apps</s5l>
        </span>
    </div>
    <div class="card-body">
        <button routerLink="../apps/configuration" disabled>
            <i class="fa fa-wrench" aria-hidden="true"></i>
            <s5l>admc.apps.configuration</s5l>
        </button>
        <button routerLink="../apps/roles">
            <i class="fa fa-users" aria-hidden="true"></i>
            <s5l>admc.apps.roles</s5l>
        </button>
        <button routerLink="../apps/calendar" disabled>
            <i class="fa fa-calendar" aria-hidden="true"></i>
            <s5l>admc.apps.calendar</s5l>
        </button>
    </div>
        
    `
})
export class AdmcAppsCardComponent extends OdeComponent {

}