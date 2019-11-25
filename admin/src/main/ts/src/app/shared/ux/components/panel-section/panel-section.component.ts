import {Component, Input} from '@angular/core';

@Component({
    selector: 'ode-panel-section',
    templateUrl: './panel-section.component.html',
    styleUrls: ['./panel-section.component.scss']
})
export class PanelSectionComponent {
    @Input('section-title') sectionTitle: string;
    @Input() folded: boolean = null;
}
