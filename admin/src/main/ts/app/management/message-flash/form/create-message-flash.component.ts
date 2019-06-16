import { Component, ChangeDetectionStrategy,} from "@angular/core";

@Component({
    selector: 'create-message-flash',
    template: `
        <message-flash-form action="create">
        </message-flash-form>
    `,
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class CreateMessageFlashComponent {

    constructor() {}

}