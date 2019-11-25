import {ChangeDetectionStrategy, Component, Input, OnDestroy, OnInit} from '@angular/core';

@Component({
    selector: 'ode-message-flash-preview',
    templateUrl: './message-flash-preview.component.html',
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class MessageFlashPreviewComponent implements OnInit, OnDestroy {

    @Input() text: string;
    @Input() color: string;
    @Input() customColor: string;

    constructor() {}

    ngOnInit() {}

    ngOnDestroy() {}

}
