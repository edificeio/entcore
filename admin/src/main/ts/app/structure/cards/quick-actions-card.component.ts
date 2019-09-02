import { Component, ChangeDetectionStrategy } from '@angular/core'

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
            <button routerLink="groups/manual/create">
                <s5l>create.group</s5l>
                <i class="fa fa-users"></i>
            </button>
            <button routerLink="users/filter" [queryParams]="{duplicates: true}">
                <s5l>manage.duplicates</s5l>
                <i class="fa fa-user-times"></i>
            </button>
            <button routerLink="management/message-flash/create">
                <s5l>create.flash.message</s5l>
                <i class="fa fa-bullhorn"></i>
            </button>
        </div>
    `,
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class QuickActionsCard {
}