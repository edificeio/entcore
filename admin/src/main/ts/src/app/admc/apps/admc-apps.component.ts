import { Component } from "@angular/core";
import { OdeComponent } from "ngx-ode-core";

@Component({
    selector: 'ode-admc-apps',
    template: `
        <h1>
            <s5l>admc.apps</s5l>
        </h1>
        <div class="tabs">
            <button 
                class="tab"
                routerLink="roles"
                routerLinkActive="active"
            >
                <s5l>admc.apps.roles</s5l>
            </button>
        </div>
        <router-outlet></router-outlet>
    `
})
export class AdmcAppsComponent extends OdeComponent {

}