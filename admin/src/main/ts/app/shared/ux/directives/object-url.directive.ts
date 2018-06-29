import { Directive, ElementRef } from '@angular/core'

/**
* 
* see https://developer.mozilla.org/en-US/docs/Web/API/URL/createObjectURL
*/
@Directive({ selector: '[object-url]' })
export class ObjectURLDirective {

    constructor(
        private elementRef: ElementRef,
    ){}

    private objectURL;

    // TODO provide more specific method like createJSON, createCSV, createPDF 
    // to free directive consumer from using Blog API
    create(blob:Blob, filename:string): void {
        if (this.objectURL !== null)
            window.URL.revokeObjectURL(this.objectURL);
        this.objectURL = window.URL.createObjectURL(blob);
        this.elementRef.nativeElement.href = this.objectURL;
        this.elementRef.nativeElement.download = filename;
    }

}
