import { Component, ChangeDetectionStrategy,} from "@angular/core";
import { ActivatedRoute, Params, Router } from '@angular/router'

@Component({
    selector: 'edit-message-flash',
    template: `
        <message-flash-form action="edit" [messageId]="messageId">
        </message-flash-form>
    `,
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class EditMessageFlashComponent {

    constructor(public route: ActivatedRoute, public router: Router) {}

    public messageId: string;

    ngOnInit(): void {
        this.route.params.subscribe((params: Params) => {
            if (params['messageId']) {
                this.messageId = params['messageId'];
            }
        });
    }

}