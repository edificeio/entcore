import { Component } from "@angular/core";
import { OdeComponent } from "ngx-ode-core";

@Component({
    selector: 'ode-admc-search',
    template: `
        <h1>
            <s5l>admc.search</s5l>
        </h1>
        <div class="tabs">
            <button 
                class="tab"
                routerLink="transverse"
                routerLinkActive="active"
            >
                <s5l>admc.search.transverse</s5l>
            </button>
        </div>
        <router-outlet></router-outlet>
    `
})
export class AdmcSearchComponent extends OdeComponent {

}