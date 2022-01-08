import { Component } from '@angular/core';
import { OdeComponent } from 'ngx-ode-core';

@Component({
    selector: 'ode-admc-communication-card',
    template: `
        <div class="card-header">
            <span>
                <s5l>admc.communications</s5l>
            </span>
        </div>
        <div class="card-body">
            <button routerLink="../communications/welcome-message" disabled>
                <i class="fa fa-pencil" aria-hidden="true"></i>
                <s5l>admc.communications.welcome-message</s5l>
            </button>
            <button routerLink="../communications/flash-message" disabled>
                <i class="fa fa-commenting" aria-hidden="true"></i>
                <s5l>admc.communications.flash-message</s5l>
            </button>
            <button routerLink="../communications/notifications" disabled>
                <i class="fa fa-bell" aria-hidden="true"></i>
                <s5l>admc.communications.notifications</s5l>
            </button>
        </div>
    `
})
export class AdmcCommunicationCardComponent extends OdeComponent {
}
