import { Component } from "@angular/core";
import { OdeComponent } from "ngx-ode-core";

@Component({
    selector: 'ode-admc-misc-card',
    template: `
    <div class="card-header">
        <span>
            <s5l>admc.misc</s5l>
        </span>
    </div>
    <div class="card-body">
        <button routerLink="../misc/xiti" disabled>
            <i class="fa fa-line-chart" aria-hidden="true"></i>
            <s5l>admc.misc.xiti</s5l>
        </button>
        <button routerLink="../misc/keys" disabled>
            <i class="fa fa-key" aria-hidden="true"></i>
            <s5l>admc.misc.keys</s5l>
        </button>
        <button routerLink="../misc/multimedia" disabled>
            <i class="fa fa-picture-o" aria-hidden="true"></i>
            <s5l>admc.misc.multimedia</s5l>
        </button>
        <button routerLink="../misc/gar">
            <i class="fa fa-wrench" aria-hidden="true"></i>
            <s5l>admc.misc.gar</s5l>
        </button>
    </div>
        
    `
})
export class AdmcMiscCardComponent extends OdeComponent {

}