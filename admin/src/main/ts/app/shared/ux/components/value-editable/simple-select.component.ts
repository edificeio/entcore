import { ChangeDetectorRef, ChangeDetectionStrategy, Component, Input } from '@angular/core'
import { BundlesService } from 'sijil'


@Component({
    selector: 'simple-select',
    template: `
    <select [(ngModel)]="model[selected]">
        <option *ngFor="let option of options" [ngValue]="option">
            {{option}}
        </option>
    </select>
    `,
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class SimpleSelectComponent { 
    constructor (
        private bundles: BundlesService,
        private cdRef : ChangeDetectorRef
    ) {}

    translate = (...args) => { return (<any> this.bundles.translate)(...args) }

    @Input() selected: string;
    @Input() model : {[key:string]: string};
    @Input() options : Array<String>;

}
