import {AfterViewInit, Component, ElementRef, EventEmitter, forwardRef, Input, OnDestroy, Output, Renderer2, ViewChild} from '@angular/core';

import {ControlValueAccessor, NG_VALUE_ACCESSOR, NgModel} from '@angular/forms';

import {LabelsService} from '../../services';
import Flatpickr from 'flatpickr';
import French from 'flatpickr/dist/l10n/fr.js';

// access ngmodel
const NOOP = () => {
};
export const CUSTOM_INPUT_CONTROL_VALUE_ACCESSOR: any = {
    provide: NG_VALUE_ACCESSOR,
    useExisting: forwardRef(() => DatepickerComponent),
    multi: true
};

@Component({
    selector: 'ode-date-picker',
    templateUrl: './datepicker.component.html',
    providers: [ CUSTOM_INPUT_CONTROL_VALUE_ACCESSOR ]
})
export class DatepickerComponent implements OnDestroy, AfterViewInit, ControlValueAccessor {

    constructor(private renderer: Renderer2,
                private labelsService: LabelsService) {}

    get value(): any {
        return this.innerValue;
    }

    set value(v: any) {
        let init = false;
        if (this.innerValue === undefined) {
            init = true;
        }
        if (v !== this.innerValue) {
            this.innerValue = v;
            this.onChangeCallback(v);
            if (!init) {
                this.changeDate.emit(v);
            }

        }
    }

    private innerValue: any = '';

    @ViewChild('datePickerElement', { static: false })
    datePickerElement: ElementRef;

    @ViewChild('inputRef', { static: false })
    inputElement: ElementRef;

    // instance flatpickr
    private datePickerInst: Flatpickr;

    @ViewChild(NgModel, { static: false })
    model: NgModel;

    @Input()
    disabled = false;

    @Input()
    enableTime = false;

    @Input()
    placeholder = '';

    @Input()
    minDate;

    @Input()
    maxDate;

    @Output()
    changeDate: EventEmitter<string> = new EventEmitter<string>();

    private onChangeCallback: (_: any) => void = NOOP;
    private onTouchedCallback: () => void = NOOP;

    ngAfterViewInit(): void {
        // add attr data-input, mandatory for the picker to work in wrap mode
        this.renderer.setAttribute(this.inputElement.nativeElement, 'data-input', '');

        // disabled case
        if (this.disabled === true) {
            this.model.control.disable();
        }

        const navigatorLanguage = navigator.language.split('-')[0];
        let datePickerLocale = {};
        if (navigatorLanguage === 'fr') {
            datePickerLocale = French.fr;
        }

        // options for the flatpickr instance
        const options = {
            altInput: !this.disabled,
            altFormat: 'd/m/Y', // date format displayed to user
            dateFormat: 'Y-m-d', // date format sent to server
            allowInput: false,
            enableTime: this.enableTime,
            minDate: this.minDate,
            maxDate: this.maxDate,
            clickOpens: !this.disabled,
            wrap: true, // to add input decoration (calendar icon and delete icon)
            locale: datePickerLocale
        };

        this.datePickerInst = new Flatpickr(this.datePickerElement.nativeElement, options);
    }

    ngOnDestroy(): void {
        this.datePickerInst.destroy();
    }

    writeValue(value: any): void {
        if (value !== this.innerValue && this.datePickerInst) {
            this.innerValue = value;
            this.datePickerInst.setDate(value);
            this.onChangeCallback(value);
        }
    }

    registerOnChange(fn: any): void {
        this.onChangeCallback = fn;
    }

    registerOnTouched(fn: any): void {
        this.onTouchedCallback = fn;
    }

    labels(label) {
        return this.labelsService.getLabel(label);
    }
}
