import { Component, ElementRef, forwardRef, Input } from '@angular/core';
import { ControlValueAccessor, NG_VALUE_ACCESSOR } from '@angular/forms';
import ClickEvent = JQuery.ClickEvent;

/* MultiSelectComponent is a rewrite of MultiComboComponent to integrate it easily in angular forms */

export interface MultiSelectOption<K> {
    label: string;
    value: K;
}

const css = {
    options: 'multi-select__options',
    optionsItem: 'multi-select__options-item',
    toggle: 'multi-select__toggle',
    toggleActive: 'multi-select__toggle--active',
    container: 'multi-select__options-container',
    actions: 'multi-select__options-actions',
    actionsItem: 'multi-select__options-actions-item',
    preview: 'multi-select__options-preview',
    previewItem: 'multi-select__options-preview-item',
    previewItemDeleteIcon: 'multi-select__options-preview-item-delete-icon',
    containerActive: 'multi-select__options-container--active',
    optionsItemSelected: 'multi-select__options-item--selected'
};
export const multiSelectClasses = css;

export const multiSelectLocators = {
    options: `.lct-${css.options}`,
    optionsItem: `.lct-${css.optionsItem}`,
    toggle: `.lct-${css.toggle}`,
    container: `.lct-${css.container}`,
    preview: `.lct-${css.preview}`,
    previewItem: `.lct-${css.previewItem}`
};

@Component({
    selector: 'multi-select',
    template: `
        <button type="button"
                (click)="toggleOptionsVisibility()" [disabled]="isDisabled"
                class="${css.toggle} lct-${css.toggle}"
                [ngClass]="{'${css.toggleActive}': isOptionsVisible}">{{label | translate}}</button>
        <div class="${css.container} lct-${css.container}"
             [ngClass]="{'${css.containerActive}': isOptionsVisible}">
            <div class="${css.actions}">
                <button type="button"
                    class="${css.actionsItem}"
                    (click)="deselectAll()">{{'ux.multiselect.deselect-all' | translate}}</button>
            </div>
            <ul class="${css.options} lct-${css.options}">
                <li class="${css.optionsItem} lct-${css.optionsItem}"
                    *ngFor="let option of options"
                    (click)="optionClicked(option)"
                    [ngClass]="{'${css.optionsItemSelected}': isSelected(option)}">{{option.label | translate}}</li>
            </ul>
        </div>
        <div class="${css.preview} lct-${css.preview}" *ngIf="preview">
            <ng-container *ngFor="let option of options">
                <button *ngIf="isSelected(option)"
                type="button"
                class="${css.previewItem} lct-${css.previewItem}"
                (click)="optionClicked(option)">
                    <span>{{option.label | translate}}</span>
                    <i class="${css.previewItemDeleteIcon} fa fa-trash is-size-5"></i>
                </button>
            </ng-container>
        </div>
    `,
    styles: [`
        :host {
            position: relative;
        }
        .${css.toggle} {
            min-width: 210px;
            outline: none;
            background-color: #f2f2f2;
        }`, `
        .${css.toggle}:after {
            content: '▼';
            float: right;
            margin-left: 10px;
            transition: transform 0.25s, color 0.25s;
        }`, `
        .${css.toggle}:hover:after {
            color: white;
        }`, `
        .${css.toggle}:hover,
        .${css.toggle}.${css.toggleActive} {
            background-color: #ff8352;
            color: white;
        }`, `
        .${css.toggle}.${css.toggleActive}:after {
            transform: rotate(180deg);
            color: white;
        }`, `
        .${css.container} {
            display: none;
            position: absolute;
            z-index: 2;
            left: 1px;
            top: 31px;
            overflow: hidden;
            background:white;
            border: 1px solid #ddd;
            padding: 0 10px 0 20px;
            min-width: 160px;
        }`, `
        .${css.container}.${css.containerActive} {
            display: block;
        }`, `
        .${css.options} {
            list-style: none;
            padding: 0;
            overflow-y: scroll;
            max-height: 200px;
            margin-bottom: 10px;
        }`, `
        .${css.optionsItem} {
            white-space: nowrap;
            cursor: pointer;
            line-height: 25px;
            font-size: 13px;
            transition: background-color 0.25s, color 0.25s;
            margin-bottom: 5px;
            padding: 0 10px;
        }`, `
        .${css.optionsItem}.${css.optionsItemSelected} {
            color: white;
            background-color: #217b9e;
        }`, `
        .${css.preview} {
            color: white;
            margin: 10px 0;
        }`, `
        .${css.previewItem} {
            border: 1px solid #217b9e;
            display: inline-block;
            font-size: 0.8em;
            margin-right: 10px;
            margin-bottom: 10px;
            padding: 10px 10px;
            cursor: pointer;
            transition: all 0.25s;
            border-radius: 3px;
            background-color: #217b9e;
            line-height: 1.5em;
            color: white;
        }`, `
        .${css.previewItem}:hover {
            background-color: #61bbde;
            border-color: white;
        }`, `
        .${css.previewItemDeleteIcon} {
            opacity: 0;
            transition: opacity 0.25s;
            float: right;
            padding-left: 10px;
        }`, `
        .${css.previewItem}:hover .${css.previewItemDeleteIcon} {
            opacity: 1;
        }`, `
        .${css.actions} {
            margin-top: 10px;
        }`, `
        .${css.actionsItem}:hover {
            background: #ffccb8;
        }`],
    providers: [
        {
            provide: NG_VALUE_ACCESSOR,
            useExisting: forwardRef(() => MultiSelectComponent),
            multi: true
        }
    ],
    host: {
        '(document:click)': 'closeIfOpened($event)'
    }
})
export class MultiSelectComponent<K> implements ControlValueAccessor {
    @Input()
    public label = '';

    @Input()
    public options: Array<MultiSelectOption<K>> = [];

    @Input()
    public preview = false;

    @Input()
    public trackByFn: (optionValue: K) => number | string;

    public model: Array<K> = [];

    public isDisabled = false;

    public isOptionsVisible = false;

    constructor(private elementRef: ElementRef) {
    }

    public optionClicked(option: MultiSelectOption<K>) {
        if (this.model) {
            const index = this.getIndexOfOptionInModel(option);
            if (index < 0) {
                this.model.push(option.value);
            } else {
                this.model.splice(index, 1);
            }
        } else {
            this.model = [option.value]
        }
        this.onChange(this.model);
    }

    public deselectAll(): void {
        this.model = [];
        this.onChange(this.model);
    }

    public toggleOptionsVisibility(): void {
        this.isOptionsVisible = !this.isOptionsVisible;
    }

    public isSelected(option: MultiSelectOption<K>): boolean {
        return this.model ? (this.getIndexOfOptionInModel(option) >= 0) : false;
    }

    public closeIfOpened(event: ClickEvent) {
        if (this.isOptionsVisible &&
            !this.elementRef.nativeElement.querySelector(multiSelectLocators.container).contains(event.target) &&
            !this.elementRef.nativeElement.querySelector(multiSelectLocators.toggle).contains(event.target)) {
            this.isOptionsVisible = false;
        }
        return true;
    }

    private getIndexOfOptionInModel(option: MultiSelectOption<K>): number {
        return this.model.map(this.trackByFn).indexOf(this.trackByFn(option.value));
    }

    private onChange = (_: Array<K>) => {
    };

    private onTouched = () => {
    };

    registerOnChange(fn: any): void {
        this.onChange = fn;
    }

    registerOnTouched(fn: any): void {
        this.onTouched = fn;
    }

    setDisabledState(isDisabled: boolean): void {
        this.isDisabled = isDisabled;
    }

    writeValue(obj: Array<K>): void {
        this.model = obj;
        this.onChange(this.model);
    }
}
