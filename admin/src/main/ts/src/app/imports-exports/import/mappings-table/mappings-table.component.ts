import { OdeComponent } from './../../../core/ode/OdeComponent';
import { ChangeDetectionStrategy, Component, EventEmitter, Input, Output, QueryList, ViewChildren, Injector } from '@angular/core';
import {BundlesService} from 'sijil';
import {ComponentDescriptor, DynamicComponentDirective} from '../../../shared/ux/directives';
import {SimpleSelectComponent} from '../../../shared/ux/components';
import {Option} from '../../../shared/ux/components/value-editable/simple-select.component';


@Component({
    selector: 'ode-mappings-table',
    templateUrl: './mappings-table.component.html',
    styleUrls: ['./mappings-table.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class MappingsTableComponent extends OdeComponent {
    constructor(
        injector: Injector,
        private bundles: BundlesService)  {
            super(injector);
        }

    @ViewChildren(DynamicComponentDirective) dComponents: QueryList<DynamicComponentDirective>;
    @Input() headers: string[];
    @Input() mappings: {};
    @Input() availables: string[];
    @Input() emptyLabel: string;
    @Input() emptyWarning: string;
    @Input() mappingsKeySort: boolean;
    @Input() type: 'user' | 'class';
    @Output() selectChange: EventEmitter<string> = new EventEmitter<string>();

    private readonly emptyValue: string[] = ['ignore', '']; // 'ignore' is use for FieldsMapping and '' for ClassesMapping

    translate = (...args) => (this.bundles.translate as any)(...args);

    isEmpty(value: string) {
        return this.emptyValue.includes(value);
    }

    mappingsKeys = function(): string[] {
        if (!this.mappings) {
          return [];
        }
        if (this.mappingsKeySort) {
            return Object.keys(this.mappings).sort();
        }
        return Object.keys(this.mappings);
    };

    newSimpleSelect = (): ComponentDescriptor => {
        let config = {};
        if (this.type === 'class') {
            config = {
                model: this.mappings,
                options : this.getAvailablesOptions(this.availables)
            };
        } else if (this.type === 'user') {
            config = {
                model: this.mappings,
                options : this.getAvailablesOptions(this.availables),
                ignoreOption: {value: 'ignore', label: this.translate('import.mapping.ignore')}
            };
        }
        return new ComponentDescriptor(SimpleSelectComponent, config);
    }

    loadAvailables(value: string, index: number) {
        this.dComponents.toArray()[index].load({selected: value});
        this.dComponents.toArray()[index].componentRef.instance.selectChange.subscribe(event => {
            this.selectChange.emit(event);
        });
    }
    selectIsLoaded(index: number): boolean {
        if (this.dComponents === undefined || this.dComponents.toArray()[index] === undefined) {
          return false;
        } else {
            return this.dComponents.toArray()[index].isLoaded();
        }
    }

    getAvailablesOptions = (availables: string[]): Option[] => {
        return availables
            .filter(available => available !== 'ignore')
            .map((field: string) => {
                return {value: field, label: this.translate(field)
            };
        }) as Option[];
    }
}
