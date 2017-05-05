import { Component, Input, ChangeDetectionStrategy } from '@angular/core'
import { StructureModel } from '../../../../models'

@Component({
    selector: 'quick-actions-card',
    template: `
        <div class="card-header">
            <span>
                <i class="fa fa-wrench"></i>
                <s5l>quick.actions</s5l>
            </span>
        </div>
        <div class="card-body">
            <button routerLink="users/create">
                <s5l>create.user</s5l>
                <i class="fa fa-user-plus"></i>
            </button>
            <button routerLink="groups">
                <s5l>create.group</s5l>
                <i class="fa fa-users"></i>
            </button>
            <button>
                <s5l>manage.duplicates</s5l>
                <i class="fa fa-user-times"></i>
            </button>
            <button>
                <s5l>manage.reports</s5l>
                <i class="fa fa-exclamation-circle"></i>
            </button>
        </div>
    `,
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class QuickActionsCard {
        @Input() structure: StructureModel
}