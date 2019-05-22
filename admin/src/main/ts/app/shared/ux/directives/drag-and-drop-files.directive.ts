import { Directive, HostListener, HostBinding, Output, EventEmitter, Input } from "@angular/core";
import { Error } from "../../../core/services";

@Directive({
    selector: '[dragAndDropFiles]'
})
export class DragAndDropFilesDirective {
    @Input()
    allowedExtensions: Array<string> = [];
    @Input()
    maxFilesNumber: number = 1;

    @Output()
    dragAndDrop: EventEmitter<File[]> = new EventEmitter();
    @Output()
    invalidDragAndDrop: EventEmitter<Error> = new EventEmitter();

    @HostBinding('style.background') 
    private backgroundColor = '#fafafa';
    @HostBinding('style.border-color') 
    private borderColor = '#ddd';

    @HostListener('dragover', ['$event'])
    public onDragOver(evt) {
        evt.preventDefault();
        evt.stopPropagation();
        this.highlightColors();
    }

    @HostListener('dragleave', ['$event'])
    public onDragLeave(evt) {
        evt.preventDefault();
        evt.stopPropagation();
        this.resetColors();
    }

    @HostListener('drop', ['$event'])
    public onDrop(event) {
        event.preventDefault();
        event.stopPropagation();

        let files = event.dataTransfer.files;
        let validFiles : Array<File> = [];
        let invalidFiles : Array<File> = [];
        
        if (files.length > this.maxFilesNumber) {
            const error: Error = new Error();
            error.message = `Only ${this.maxFilesNumber} file(s) allowed`;
            console.error(error);
            this.invalidDragAndDrop.emit(error);
        } else {
            for (let i = 0; i < files.length; i++) {
                let filenameSplit = files[i].name.split('.');
                let ext = filenameSplit[filenameSplit.length - 1];
                if (this.allowedExtensions.lastIndexOf(ext) != -1) {
                    validFiles.push(files[i]);
                } else {
                    invalidFiles.push(files[i]);
                }
            }
    
            if (validFiles.length > 0) {
                this.dragAndDrop.emit(validFiles);
            }
    
            if (invalidFiles.length > 0) {
                const error: Error = new Error();
                error.message = `Extension not allowed. Allowed extensions: ${this.allowedExtensions}`
                console.error(error);
                this.invalidDragAndDrop.emit(error);
            }
        }

        this.resetColors();
    }

    private resetColors() {
        this.backgroundColor = '#fafafa';
        this.borderColor = '#ddd';
    }

    private highlightColors() {
        this.backgroundColor = '#ffd1b6';
        this.borderColor = '#ff8352';
    }
}