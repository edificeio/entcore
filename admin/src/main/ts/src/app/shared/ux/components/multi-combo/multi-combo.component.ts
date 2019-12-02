import { OdeComponent } from './../../../../core/ode/OdeComponent';
import { Component, ElementRef, EventEmitter, Input, Output, Injector } from '@angular/core';
import {LabelsService} from '../../services';

/* If you need to use multi-combo in a form you should give a look to the MultiSelectComponent. */

@Component({
    selector: 'ode-multi-combo',
    templateUrl: './multi-combo.component.html',
    styleUrls: ['./multi-combo.component.scss'],
    host: {
        '(document:click)': 'onClick($event)',
    }
})
export class MultiComboComponent extends OdeComponent {

    constructor(
        injector: Injector,
        private _eref: ElementRef,
        private labelsService: LabelsService) {
            super(injector);
        }

    @Input()
    set comboModel(model) {
        this._comboModel = model;

        if (!model) {
            this.filteredModel = [];
            return;
        }

        for (let i = 0; i < this.filteredModel.length; i++) {
            const idx = model.indexOf(this.filteredModel[i]);
            if (idx < 0) {
                this.filteredModel.splice(idx, 1);
                i--;
            }
        }
    }
    @Input('outputModel') filteredModel = [];
    @Input() title = 'Select';
    @Input() display: string | Function;
    @Input() filter: string;
    @Input() orderBy: (string | Array<(string | Function)> | Function);
    @Input() reverse = false;
    @Input('max') maxSelected: number;
    @Input() disabled = false;

    @Output() onSelectItem = new EventEmitter<any>();
    @Output() onDeselectItem = new EventEmitter<any>();
    @Output('outputModelChange') filteredModelChange = new EventEmitter<any>();
    @Output() onOpen = new EventEmitter<any>();
    @Output() onClose = new EventEmitter<any>();

    self = this;
    _comboModel = [];

    filteredComboModel = [];

    search = {
        input: '',
        reset() {
            this.input = '';
        }
    };

    show = false;

    labels(label) {
        return this.labelsService.getLabel(label);
    }

    toggleVisibility() {
        this.show = !this.show;
        if (this.show) {
            this.search.reset();
            this.onOpen.emit();
        } else {
             this.onClose.emit();
        }
    }

    isSelected(item) {
        return this.filteredModel && this.filteredModel.indexOf(item) >= 0;
    }

    isDisabled() {
        return this.filteredModel && this.maxSelected &&
            this.maxSelected <= this.filteredModel.length;
    }

    toggleItem(item) {
        const idx = this.filteredModel.indexOf(item);
        if (idx >= 0) {
            this.filteredModel.splice(idx, 1);
            this.onDeselectItem.emit(item);
        } else if (!this.maxSelected ||
                this.filteredModel.length < this.maxSelected) {
            this.filteredModel.push(item);
            this.onSelectItem.emit(item);
        }
        this.filteredModelChange.emit(this.filteredModel);
    }

    selectAll() {
        this.filteredModel = [];
        for (let i = 0; i < this.filteredComboModel.length; i++) {
            this.filteredModel.push(this.filteredComboModel[i]);
        }
        this.filteredModelChange.emit(this.filteredModel);
    }

    deselectAll() {
        this.filteredModel = [];
        this.filteredModelChange.emit(this.filteredModel);
    }

    displayItem(item) {
        return  item instanceof Object ?
                    this.display && typeof this.display === 'string' ?
                        item[this.display] :
                        item.toString() :
                    this.display && this.display instanceof Function ?
                        this.display(item || '') :
                        item;
    }

    onClick(event) {
        if (this.show && !this._eref.nativeElement.contains(event.target)) {
            this.toggleVisibility();
        }
        return true;
    }

    getFilter() {
        if (!this.filter) {
            return '';
        }
        let filter: string | {} = '';
        if (this._comboModel.length > 0 ) {
            if (typeof this._comboModel[0] === 'string') {
                filter = this.search.input;
            } else {
                filter = {};
                filter[this.filter] = this.search.input;
            }
        }
        return filter;
    }

}
