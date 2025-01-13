import { Component } from "@angular/core";
import { OdeComponent } from "ngx-ode-core";

@Component({
    selector: 'ode-admc-misc',
    template: `
        <h1>
            <s5l>admc.misc</s5l>
        </h1>
        <div class="tabs">
            <button 
                class="tab"
                routerLink="gar"
                routerLinkActive="active"
            >
                <s5l>admc.misc.gar</s5l>
            </button>
        </div>
        <router-outlet></router-outlet>
    `
})
export class AdmcMiscComponent extends OdeComponent {

}