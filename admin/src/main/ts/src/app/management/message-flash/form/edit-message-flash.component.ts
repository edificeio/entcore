import { OdeComponent } from './../../../core/ode/OdeComponent';
import { ChangeDetectionStrategy, Component, OnInit, Injector } from '@angular/core';
import {ActivatedRoute, Params, Router} from '@angular/router';

@Component({
    selector: 'ode-edit-message-flash',
    template: `
        <ode-message-flash-form action="edit" [messageId]="messageId">
        </ode-message-flash-form>
    `,
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class EditMessageFlashComponent extends OdeComponent implements OnInit {

    constructor(injector: Injector) {
        super(injector);
    }

    public messageId: string;

    ngOnInit(): void {
        super.ngOnInit();
        this.route.params.subscribe((params: Params) => {
            if (params.messageId) {
                this.messageId = params.messageId;
            }
        });
    }

}
