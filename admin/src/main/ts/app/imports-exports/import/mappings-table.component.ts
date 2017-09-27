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
        <thead>
            <th>{{ headers[0] | translate }}</th>
            <th>{{ headers[1] | translate }}</th>
        </thead>
        <tbody>
        <tr *ngFor="let value of mappingsKeys(),index as i;">
                <td>{{value}}</td>
                <td (click)="loadAvailables(value, i)">
                    <span [hidden]="isLoaded(i)">
                        {{mappings[value]}}
                    </span>
                    <ng-template [dynamic-component]="newSimpleSelect()"></ng-template>
                </td>
            </tr>
        </tbody>
    </table>
    `,
    styles: [`
        td:first-of-type + td { font-weight : bold; }
        td:first-of-type + td:hover { border: 2px dashed orange; cursor:pointer; }
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
    isLoaded(index:number):boolean {
        if (this.dComponents == undefined) 
            return false;
        else
            return this.dComponents.toArray()[index].isLoaded();
    }

    ngOnInit() {}

    ngAfterViewInit() {}
}
