import { ChangeDetectionStrategy, Component, Injector, Input } from '@angular/core';
import { OdeComponent } from 'ngx-ode-core';
import { StructureModel } from 'src/app/core/store/models/structure.model';

@Component({
    selector: 'ode-imports-exports-card',
    templateUrl: './imports-exports-card.component.html',
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class ImportsExportsCardComponent extends OdeComponent {
    @Input() structure: StructureModel;
    constructor(injector: Injector) {
        super(injector);
    }
}
