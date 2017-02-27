import { Component, Input, ChangeDetectionStrategy } from '@angular/core'
import { StructureModel } from '../../../../store'

@Component({
    selector: 'structure-card',
    template: `
        <div class="card-header">
            <span>
                <i class="fa fa-building"></i>
                <s5l>structure</s5l>
            </span>
        </div>
        <div class="card-body">
            <button>
                <i class="fa fa-plug"></i>
                <s5l>manage.connectors</s5l>
            </button>
            <button>
                <i class="fa fa-window-maximize"></i>
                <s5l>manage.widgets</s5l>
            </button>
            <button>
                <i class="fa fa-th"></i>
                <s5l>manage.applications</s5l>
            </button>
        </div>
    `,
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class StructureCard {
    @Input() structure: StructureModel
}