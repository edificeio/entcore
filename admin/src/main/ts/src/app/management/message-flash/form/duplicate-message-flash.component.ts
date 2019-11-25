import {ChangeDetectionStrategy, Component, OnInit,} from '@angular/core';
import {ActivatedRoute, Params, Router} from '@angular/router';

@Component({
    selector: 'ode-duplicate-message-flash',
    template: `
        <ode-message-flash-form action="duplicate" [messageId]="messageId">
        </ode-message-flash-form>
    `,
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DuplicateMessageFlashComponent implements OnInit{

    constructor(public route: ActivatedRoute, public router: Router) {}

    public messageId: string;

    ngOnInit(): void {
        this.route.params.subscribe((params: Params) => {
            if (params.messageId) {
                this.messageId = params.messageId;
            }
        });
    }

}
