import { ChangeDetectionStrategy, ChangeDetectorRef, Component, Input, OnInit } from '@angular/core'
import { BundlesService } from 'sijil'


@Component({
    selector: 'mappings-table',
    template: `
    <table>
        <tr>
            <th>{{ headers[0] | translate }}</th>
            <th>{{ headers[1] | translate }}</th>
        </tr>
        <tr *ngFor="let value of mappingsKeys">
            <td>{{value}}</td>
            <td>
            <select [(ngModel)]="mappings[value]" name="availables">
                <option *ngFor="let available of availables" [ngValue]="available">
                    {{available}}
                </option>
            </select>
            </td>
        </tr>
    </table>
    `,
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class MappingsTable implements OnInit { 
    constructor (
        private bundles: BundlesService,
        private cdRef : ChangeDetectorRef)  {}
    
    translate = (...args) => { return (<any> this.bundles.translate)(...args) }

    @Input() headers : Array<String>;
    @Input() mappings : Object; // TODO type with a Map<> when available in Typescript
    mappingsKeys : Array<String>
    @Input() availables : Array<String>;

    ngOnInit() {
        this.mappingsKeys = Object.keys(this.mappings);
    }
}
