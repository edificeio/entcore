import { Component, ChangeDetectionStrategy } from '@angular/core'

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
            <button routerLink="imports-exports/export">
                <i class="fa fa-arrow-up"></i>
                <s5l>export.accounts</s5l>
            </button>
            <button disabled title="En construction">
                <i class="fa fa-arrow-down"></i>
                <s5l>import.users</s5l>
            </button>
            <button routerLink="imports-exports/massmail">
                <i class="fa fa-files-o"></i>
                <s5l>massmail.accounts</s5l>
            </button>
        </div>
    `,
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class ImportsExportsCard {
}
