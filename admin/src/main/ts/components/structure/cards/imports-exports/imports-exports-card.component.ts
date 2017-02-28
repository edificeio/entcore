import { Component, Input, ChangeDetectionStrategy } from '@angular/core'
import { StructureModel } from '../../../../store'

@Component({
    selector: 'imports-exports-card',
    template: `
        <div class="card-header">
            <span>
                <i class="fa fa-exchange "></i>
                <s5l>imports.exports</s5l>
            </span>
        </div>
        <div class="card-body">
            <button>
                <i class="fa fa-arrow-up"></i>
                <s5l>export.accounts</s5l>
            </button>
            <button>
                <i class="fa fa-arrow-down"></i>
                <s5l>import.users</s5l>
            </button>
            <button>
                <i class="fa fa-files-o"></i>
                <s5l>massmail.accounts</s5l>
            </button>
        </div>
    `,
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class ImportsExportsCard {
    @Input() structure: StructureModel
}