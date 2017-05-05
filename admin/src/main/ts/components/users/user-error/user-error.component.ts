import { Component, Input, ChangeDetectionStrategy } from '@angular/core'

@Component({
    selector: 'user-error',
    template: `
        <div class="panel-header">
            <i class="fa fa-warning"></i>
            <span><s5l>user.root.error</s5l></span>
        </div>
        <div class="padded">
            <s5l>user.root.error.text</s5l>
            <div class="error-tech"  *ngIf="error">
                <span>
                    {{ error?.stack || error }}
                </span>
            </div>
        </div>
    `,
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class UserError {

    private error : Error

}