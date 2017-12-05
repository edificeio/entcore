import { Component, Input, OnInit, Output, EventEmitter, Renderer, ElementRef } from '@angular/core'
import { LabelsService } from '../services'

@Component({
    selector:'multi-combo',
    template: `
        <button (click)="toggleVisibility()"
            [ngClass]="{ opened: show }"
            [disabled]="disabled">
            {{ title }}
        </button>
        <div [ngClass]="{ hidden: !show }">
            <div class="options">
                <button class="select-all" (click)="selectAll()" *ngIf="!maxSelected"
                    [title]="labels('select.all')">{{ labels('select.all') }}</button>
                <button class="deselect-all" (click)="deselectAll()"
                    [title]="labels('deselect.all')">{{ labels('deselect.all') }}</button>
            </div>
            <div *ngIf="filter" class="filter">
                <search-input (onChange)="search.input = $event" [attr.placeholder]="labels('search')"></search-input>
            </div>
            <ul>
                <li *ngFor="let item of _comboModel | filter: getFilter() | orderBy: orderBy | store: self:'filteredComboModel'"
                    (click)="toggleItem(item)"
                    [ngClass]="{ selected: isSelected(item) }"
                    [attr.disabled]="isDisabled()">
                    {{ displayItem(item) }}
                </li>
            </ul>
        </div>
    `,
    styles: [`
        :host {
            position: relative;
        }

        :host > button {
            min-width: 150px;
        }

        :host > div {
            position: absolute;
            z-index: 2;
            left: 0px;
            overflow: hidden;
            background:white;
            border: 2px solid black;
        }

        :host > div.hidden {
            max-height: 0px;
            border-width: 0px;
        }

        :host > div>div.options {
        }

        :host > div>div.options>* {
            display: inline-block;
            vertical-align: middle;
        }

        :host > div>div.options>button {
        }
        :host > div>div.options>button:hover {
        }

        :host > div>div.filter {
            margin: 10px 0px;
            width: 100%;
            position: relative;
        }

        :host > div>div.filter input {
            width: 100%;
        }

        :host > div>div.filter input:focus {
        }

        :host > div>ul {
            list-style: none;
            padding: 0px;
            overflow-y: scroll;
            max-height: 200px;
        }

        :host > div>ul>li {
            white-space: nowrap;
            cursor: pointer;
        }

        :host > div>ul>li.selected {
        }

        :host > div>ul>li:not(.selected):not([disabled]):hover {
        }

        :host > div>ul>li:not(.selected)[disabled="true"] {
            pointer-events: none;
        }
    `],
    host: {
        '(document:click)': 'onClick($event)',
    }
})
export class MultiComboComponent {
    @Input("outputModel") filteredModel = []
    @Input() title: string = "Select"
    @Input() display: string | Function
    @Input() filter: string
    @Input() orderBy: (string | Array<(string | Function)> | Function)
    @Input() reverse: boolean = false
    @Input("max") maxSelected: number
    @Input() disabled: boolean = false

    @Output() onSelectItem = new EventEmitter<any>()
    @Output() onDeselectItem = new EventEmitter<any>()
    @Output("outputModelChange") filteredModelChange = new EventEmitter<any>()
    @Output() onOpen = new EventEmitter<any>()
    @Output() onClose = new EventEmitter<any>()
    
    self = this
    _comboModel = []

    filteredComboModel = []

    constructor(
        private _eref: ElementRef,
        private _renderer: Renderer,
        private labelsService: LabelsService){}
    
    labels(label){
        return this.labelsService.getLabel(label)
    }

    @Input()
    set comboModel(model){
        this._comboModel = model

        if (!model) {
            this.filteredModel = []
            return
        }

        for (let i = 0; i < this.filteredModel.length; i++) {
            var idx = model.indexOf(this.filteredModel[i])
            if (idx < 0) {
                this.filteredModel.splice(idx, 1)
                i--
            }
        }
    }

    search = {
        input: '',
        reset: function() {
            this.input = ""
        }
    }

    show = false
    
    toggleVisibility() {
        this.show = !this.show
        if(this.show) {
            this.search.reset()
            this.onOpen.emit()
        } else {
             this.onClose.emit()
        }
    }

    isSelected(item) {
        return this.filteredModel && this.filteredModel.indexOf(item) >= 0
    }

    isDisabled() {
        return this.filteredModel && this.maxSelected &&
            this.maxSelected <= this.filteredModel.length
    }

    toggleItem(item) {
        let idx = this.filteredModel.indexOf(item)
        if (idx >= 0) {
            this.filteredModel.splice(idx, 1);
            this.onDeselectItem.emit(item)
        } else if (!this.maxSelected ||
                this.filteredModel.length < this.maxSelected) {
            this.filteredModel.push(item);
            this.onSelectItem.emit(item)
        }
        this.filteredModelChange.emit(this.filteredModel)
    }

    selectAll() {
        this.filteredModel = []
        for (let i = 0; i < this.filteredComboModel.length; i++) {
            this.filteredModel.push(this.filteredComboModel[i])
        }
        this.filteredModelChange.emit(this.filteredModel)
    }

    deselectAll() {
        this.filteredModel = []
        this.filteredModelChange.emit(this.filteredModel)
    }

    displayItem(item) {
        return  item instanceof Object ?
                    this.display && typeof this.display === "string" ?
                        item[this.display] :
                        item.toString() :
                    this.display && this.display instanceof Function ?
                        this.display(item || '') :
                        item
    }

    onClick(event) {
        if (this.show && !this._eref.nativeElement.contains(event.target)) {
            this.toggleVisibility()
        }
        return true
    }

    getFilter() {
        if(!this.filter)
            return ""
        let filter : string | {} = ""
        if(this._comboModel.length > 0 ){
            if(typeof this._comboModel[0] === 'string'){
                filter = this.search.input
            } else {
                filter = {}
                filter[this.filter] = this.search.input
            }
        }
        return filter
    }

}
