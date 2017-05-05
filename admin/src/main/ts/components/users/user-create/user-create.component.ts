import { Component, ChangeDetectionStrategy } from '@angular/core'

@Component({
    selector: 'user-create',
    template: `
        <div class="panel-header">
            <i class="fa">+</i>
            <span><s5l>new.user.creation</s5l></span>
        </div>
    `,
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class UserCreate {

}