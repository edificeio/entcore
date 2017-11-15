import { Component, ChangeDetectionStrategy } from '@angular/core'

@Component({
    selector: 'services-card',
    template: `
        <div class="card-header">
            <span>
                <i class="fa fa-th"></i>
                <s5l>services.card.title</s5l>
            </span>
        </div>
        <div class="card-body">
            <button routerLink="services/connectors">
                <i class="fa fa-plug"></i>
                <s5l>services.card.connectors</s5l>
            </button>
            <button disabled title="En construction">
                <i class="fa fa-window-maximize"></i>
                <s5l>services.card.widgets</s5l>
            </button>
            <button routerLink="services/applications">
                <i class="fa fa-th"></i>
                <s5l>services.card.applications</s5l>
            </button>
        </div>
    `,
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class ServicesCard {
}