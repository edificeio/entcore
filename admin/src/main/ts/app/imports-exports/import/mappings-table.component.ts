import {
    ChangeDetectionStrategy,
    Component,
    Input,
    OnInit
} from '@angular/core'
import { BundlesService } from 'sijil'


@Component({
    selector: 'mappings-table',
    template: `
    <table>
        <thead>
            <th>{{ headers[0] | translate }}</th>
            <th>{{ headers[1] | translate }}</th>
        </thead>
        <tbody>
            <tr *ngFor="let value of mappingsKeys()">
                <td>{{value}}</td>
                <td>
                <select [(ngModel)]="mappings[value]" name="availables">
                    <option *ngFor="let available of availables" [ngValue]="available">
                        {{available}}
                    </option>
                </select>
                </td>
            </tr>
        </tbody>
    </table>
    `,
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class MappingsTable implements OnInit {
    constructor (
        private bundles: BundlesService)  {}
    
    translate = (...args) => { return (<any> this.bundles.translate)(...args) };

    @Input() headers : Array<String>;
    @Input() mappings : Object; // TODO type with a Map<> when available in Typescript
    @Input() availables : Array<string>;

    ngOnInit() {
    }

    mappingsKeys = function() : Array<String> {
        if (!this.mappings) 
            return [];
        return Object.keys(this.mappings);
    }
}
