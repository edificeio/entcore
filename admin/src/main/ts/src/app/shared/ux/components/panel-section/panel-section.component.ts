import { OdeComponent } from './../../../../core/ode/OdeComponent';
import { Component, Input, Injector } from '@angular/core';

@Component({
    selector: 'ode-panel-section',
    templateUrl: './panel-section.component.html',
    styleUrls: ['./panel-section.component.scss']
})
export class PanelSectionComponent extends OdeComponent {
    @Input('section-title') sectionTitle: string;
    @Input() folded: boolean = null;
    constructor(injector: Injector) {
        super(injector);
    }
}
