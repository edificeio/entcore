import { Component } from "@angular/core";
import { OdeComponent } from "ngx-ode-core";

@Component({
    selector: 'ode-admc-search-card',
    template: `
        <div class="card-header">
            <span>
                <s5l>admc.search</s5l>
            </span>
        </div>
        <div class="card-body">
            <button routerLink="../search/transverse">
                <i class="fa fa-search" aria-hidden="true"></i>
                <s5l>admc.search.transverse</s5l>
            </button>
            <button routerLink="../search/unlinked">
                <i class="fa fa-user" aria-hidden="true"></i>
                <s5l>admc.search.unlinked</s5l>
            </button>
        </div>
    `
})
export class AdmcSearchCardComponent extends OdeComponent {

}