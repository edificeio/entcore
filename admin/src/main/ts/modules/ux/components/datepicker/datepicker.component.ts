import { Component, forwardRef, ViewChild, ElementRef, Input, OnDestroy, AfterViewInit, Renderer } from '@angular/core'

import { NgModel } from '@angular/forms';

import { NG_VALUE_ACCESSOR, ControlValueAccessor } from '@angular/forms';

const NOOP = () => {
};

export const CUSTOM_INPUT_CONTROL_VALUE_ACCESSOR: any = {
    provide: NG_VALUE_ACCESSOR,
    useExisting: forwardRef(() => Datepicker),
    multi: true
};

import Flatpickr from 'flatpickr';

@Component({
    selector: 'date-picker',
    template: `
        <div class="flatpickr" #datePickerElement>
            <input type="date" [(ngModel)]="value" [ngClass]="{ 'cursor-default': disabled }" placeholder="{{ placeholder }}" #inputRef>
            <a *ngIf="!disabled" data-toggle><i class="fa fa-calendar" aria-hidden="true"></i></a>
            <a *ngIf="!disabled" data-clear><i class="fa fa-times" aria-hidden="true"></i></a>
        </div>
    `,
    styles: ['@import url("/admin/public/styles/flatpickr.min.css")'],
    providers: [ CUSTOM_INPUT_CONTROL_VALUE_ACCESSOR ]
})
export class Datepicker implements OnDestroy, AfterViewInit, ControlValueAccessor {

    private innerValue: any = ''

    @ViewChild("datePickerElement") 
    datePickerElement: ElementRef
    
    @ViewChild("inputRef")
    inputElement: ElementRef

    // instance flatpickr
    private datePickerInst: Flatpickr

    @ViewChild(NgModel) 
    model: NgModel
    
    @Input()
    disabled: boolean = false
    
    @Input()
    enableTime: boolean = false
    
    @Input()
    placeholder: string = ''
    
    @Input()
    minDate
    
    @Input()
    maxDate

    constructor(private renderer: Renderer) {}

    get value(): any {
        return this.innerValue
    }

    set value(v: any) {
        if (v !== this.innerValue) {
            this.innerValue = v
            this.onChangeCallback(v)
        }
    }

    ngAfterViewInit(): void {
        // add attr data-input, mandatory for the picker to work in wrap mode
        this.renderer.setElementAttribute(this.inputElement.nativeElement, 'data-input', '');

        // disabled case
        if (this.disabled === true) {
            this.model.control.disable()
        }

        // options for the flatpickr instance
        let options = {
            altInput: !this.disabled,
            altFormat: 'd/m/Y', // date format displayed to user
            dateFormat: 'Y-m-d', // date format sent to server
            allowInput: false,
            enableTime: this.enableTime,
            minDate: this.minDate,
            maxDate: this.maxDate,
            clickOpens: !this.disabled,
            wrap: true // to add input decoration (calendar icon and delete icon)
        }

        this.datePickerInst = new Flatpickr(this.datePickerElement.nativeElement, options)
    }

    ngOnDestroy(): void {
        this.datePickerInst.destroy()
    }

    writeValue(value: any): void {
        if (value !== this.innerValue) {
            this.innerValue = value
            this.datePickerInst.setDate(value)
        }
    }

    registerOnChange(fn: any): void {
        this.onChangeCallback = fn
    }

    registerOnTouched(fn: any): void {
        this.onTouchedCallback = fn;
    }

    private onChangeCallback: (_: any) => void = NOOP
    private onTouchedCallback: () => void = NOOP
}