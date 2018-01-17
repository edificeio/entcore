import { 
    ChangeDetectionStrategy, ChangeDetectorRef, AfterViewInit,
    Component, Input, OnInit, ComponentRef, ViewChildren, QueryList } from '@angular/core'
import { BundlesService } from 'sijil'
import { ComponentDescriptor, DynamicComponentDirective } from '../../shared/ux/directives'
import { SimpleSelectComponent } from '../../shared/ux/components'


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
                        {{mappings[value]}}
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
export class MappingsTable implements OnInit, AfterViewInit { 
    constructor (
        private bundles: BundlesService)  {}
    
    translate = (...args) => { return (<any> this.bundles.translate)(...args) };

    @ViewChildren(DynamicComponentDirective) dComponents:QueryList<DynamicComponentDirective>;
    @Input() headers : String[];
    @Input() mappings : Object;
    @Input() availables : String[];
    @Input() emptyLabel : String;
    @Input() emptyWarning : String;

    private readonly emptyValue:String[] = ['ignore', '']; // 'ignore' is use for FieldsMapping and '' for ClassesMapping

    isEmpty(value:string) {
        return this.emptyValue.includes(value);
    }

    mappingsKeys = function() : String[] {
        if (!this.mappings) 
            return [];
        return Object.keys(this.mappings);
    }

    newSimpleSelect():ComponentDescriptor {
        return new ComponentDescriptor(SimpleSelectComponent, {model: this.mappings, options : this.availables});
    }

    loadAvailables(value:string, index:number) {
        this.dComponents.toArray()[index].load({selected:value});
    }
    selectIsLoaded(index:number):boolean {
        if (this.dComponents == undefined) 
            return false;
        else
            return this.dComponents.toArray()[index].isLoaded();
    }

    ngOnInit() {}

    ngAfterViewInit() {}
}
