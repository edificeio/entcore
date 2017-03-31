import { Component, ViewChild, Input, ElementRef, OnDestroy, AfterViewInit } from '@angular/core'

import Flatpickr from 'flatpickr';

@Component({
    selector: 'datepicker',
    template: `
        <div class="flatpickr" #datePickerElement>
            <ng-content></ng-content>
            <a *ngIf="!disabled" data-toggle><i class="fa fa-calendar" aria-hidden="true"></i></a>
            <a *ngIf="!disabled" data-clear><i class="fa fa-times" aria-hidden="true"></i></a>
        </div>
    `,
    styles: ['@import url("/admin/public/styles/flatpickr.min.css")']
})
export class Datepicker implements OnDestroy, AfterViewInit {
    
    @ViewChild("datePickerElement") datePickerElement: ElementRef
    private datePickerInst: Flatpickr
    private disabled: boolean = false

    @Input() enableTime: boolean = false
    @Input() minDate
    @Input() maxDate

    constructor() {}

    ngAfterViewInit(): void {
        let inputElem: HTMLElement = this.datePickerElement.nativeElement.querySelector("input")

        // disabled case
        if (inputElem.hasAttribute('disabled') === true) {
            this.disabled = true
            inputElem.style['cursor'] = 'default'
        }

        // add attr data-input, mandatory for the picker to work in wrap mode
        inputElem.setAttribute('data-input', '')

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
}