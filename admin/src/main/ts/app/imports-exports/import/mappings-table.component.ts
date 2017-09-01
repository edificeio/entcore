import {
    ChangeDetectionStrategy,
    ChangeDetectorRef,
    Component,
    Input,
    OnChanges,
    OnInit,
    SimpleChanges
} from '@angular/core'
import { BundlesService } from 'sijil'
import { SelectOption } from "../../shared/ux/components/multi-select.component";


@Component({
    selector: 'mappings-table',
    template: `
    <table>
        <tr>
            <th>{{ headers[0] | translate }}</th>
            <th>{{ headers[1] | translate }}</th>
        </tr>
        <tr *ngFor="let value of mappingsKeys()">
            <td>{{value}}</td>
            <td>
            <mono-select [(ngModel)]="mappings[value]" name="availables" [options]="availablesOptions">
            </mono-select>
            </td>
        </tr>
    </table>
    `,
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class MappingsTable implements OnInit, OnChanges {
    constructor (
        private bundles: BundlesService,
        private cdRef : ChangeDetectorRef)  {}
    
    translate = (...args) => { return (<any> this.bundles.translate)(...args) };

    @Input() headers : Array<String>;
    @Input() mappings : Object; // TODO type with a Map<> when available in Typescript
    @Input() availables : Array<string>;

    public availablesOptions: SelectOption<string>[] = [];
    ngOnInit() {
        this.onChange();
    }

    ngOnChanges(changes: SimpleChanges): void {
        this.onChange();
    }

    private onChange() {
        this.availablesOptions = this.availables.map(a => ({value: a, label: a}));
        this.cdRef.detectChanges();
    }

    mappingsKeys = function() : Array<String> {
        if (!this.mappings) 
            return [];
        return Object.keys(this.mappings);
    }
}
