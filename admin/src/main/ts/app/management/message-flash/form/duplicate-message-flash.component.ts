import { Component, ChangeDetectionStrategy,} from "@angular/core";
import { ActivatedRoute, Params, Router } from '@angular/router'

@Component({
    selector: 'duplicate-message-flash',
    template: `
        <message-flash-form action="duplicate" [messageId]="messageId">
        </message-flash-form>
    `,
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DuplicateMessageFlashComponent {

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