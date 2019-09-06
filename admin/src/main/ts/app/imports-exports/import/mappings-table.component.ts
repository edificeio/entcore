import { 
    ChangeDetectionStrategy, ChangeDetectorRef, AfterViewInit,
    Component, Input, OnInit, ViewChildren, QueryList, Output, EventEmitter } from '@angular/core'
import { BundlesService } from 'sijil'
import { ComponentDescriptor, DynamicComponentDirective } from '../../shared/ux/directives'
import { SimpleSelectComponent } from '../../shared/ux/components'
import { Option } from '../../shared/ux/components/value-editable/simple-select.component';


@Component({
    selector: 'mappings-table',
    template: `
    <table>
        <colgroup>
            <col />
            <col />
            <col />
        </colgroup>
        <thead>
            <th>{{ headers[0] | translate }}</th>
            <th>{{ headers[1] | translate }}</th>
            <th></th>
        </thead>
        <tbody>
        <tr *ngFor="let value of mappingsKeys(),index as i;">
                <td>{{value}}</td>
                <td (click)="loadAvailables(value, i)">
                    <span *ngIf="!isEmpty(mappings[value]); else elseBlock" [hidden]="selectIsLoaded(i)">
                        {{mappings[value] | translate}}
                    </span>
                    <ng-template #elseBlock>
                        <span [hidden]="selectIsLoaded(i)">
                            {{ emptyLabel | translate:{value:value} }}
                        </span>
                     </ng-template>
                    <ng-template [dynamic-component]="newSimpleSelect()"></ng-template>
                </td>
                <td>
                <message-sticker *ngIf="isEmpty(mappings[value])" 
                    [type]="'warning'" 
                    [messages]="[[emptyWarning, {value:value}]]">
                </message-sticker>
                </td >
            </tr>
        </tbody>
    </table>
    `,
    styles: [`
        td:first-of-type + td { font-weight : bold; }
        td:first-of-type + td:hover { border: 2px dashed orange; cursor:pointer; }
        col:last-child { width: 30px; }
        td:last-child { font-size : 1.5em; white-space: normal; }
    `],
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class MappingsTable { 
    constructor (
        private bundles: BundlesService)  {}
    
    translate = (...args) => { return (<any> this.bundles.translate)(...args) };

    @ViewChildren(DynamicComponentDirective) dComponents:QueryList<DynamicComponentDirective>;
    @Input() headers : String[];
    @Input() mappings : Object;
    @Input() availables : String[];
    @Input() emptyLabel : String;
    @Input() emptyWarning : String;
    @Input() mappingsKeySort: boolean;
    @Input() type: 'user' | 'class';
    @Output() selectChange: EventEmitter<string> = new EventEmitter<string>();

    private readonly emptyValue:String[] = ['ignore', '']; // 'ignore' is use for FieldsMapping and '' for ClassesMapping

    isEmpty(value:string) {
        return this.emptyValue.includes(value);
    }

    mappingsKeys = function() : String[] {
        if (!this.mappings) 
            return [];
        if (this.mappingsKeySort) {
            return Object.keys(this.mappings).sort();
        }
        return Object.keys(this.mappings);
    }

    newSimpleSelect = (): ComponentDescriptor => {
        let config = {};
        if (this.type == 'class') {
            config = {
                model: this.mappings, 
                options : this.getAvailablesOptions(this.availables)
            }
        } else if (this.type == 'user') {
            config = {
                model: this.mappings, 
                options : this.getAvailablesOptions(this.availables), 
                ignoreOption: {value: 'ignore', label: this.translate('import.mapping.ignore')}
            }
        }
        return new ComponentDescriptor(SimpleSelectComponent, config);
    }

    loadAvailables(value:string, index:number) {
        this.dComponents.toArray()[index].load({selected:value});
        this.dComponents.toArray()[index].componentRef.instance.selectChange.subscribe(event => {
            this.selectChange.emit(event);
        });
    }
    selectIsLoaded(index:number):boolean {
        if (this.dComponents == undefined || this.dComponents.toArray()[index] == undefined) 
            return false;
        else {
            return this.dComponents.toArray()[index].isLoaded();
        }
    }

    getAvailablesOptions = (availables: String[]): Option[] => {
        return availables
            .filter(available => available !== 'ignore')
            .map((field: String) => {
                return {value: field, label: this.translate(field)
            }
        }) as Option[];
    }
}
