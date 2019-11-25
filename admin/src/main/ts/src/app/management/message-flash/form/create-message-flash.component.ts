import {ChangeDetectionStrategy, Component, } from '@angular/core';

@Component({
    selector: 'ode-create-message-flash',
    template: `
        <ode-message-flash-form action="create">
        </ode-message-flash-form>
    `,
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class CreateMessageFlashComponent {

    constructor() {}

}
