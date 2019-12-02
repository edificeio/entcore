import { OdeComponent } from './../../../../core/ode/OdeComponent';
import { Component, ElementRef, EventEmitter, Input, OnInit, Output, ViewChild, Injector } from '@angular/core';
import {InputFileService} from '../../services/inputFile.service';

@Component({
    selector: 'ode-upload-files',
    templateUrl: './upload-files.component.html',
    styleUrls: ['./upload-files.component.scss']
})
export class UploadFilesComponent extends OdeComponent implements OnInit {
    @Input()
    fileSrc: string;
    @Input()
    allowedExtensions: Array<string>;
    @Input()
    maxFilesNumber: number;
    @Input()
    disabled: boolean;

    @Output()
    upload: EventEmitter<File[]> = new EventEmitter();
    @Output()
    invalidUpload: EventEmitter<string> = new EventEmitter();

    @ViewChild('inputFileRef', { static: false })
    inputFileRef: ElementRef;

    public multiple: boolean;

    constructor(injector: Injector,
                public inputFileService: InputFileService) {
            super(injector);
    }

    public ngOnInit(): void {
        super.ngOnInit();
        this.multiple = this.maxFilesNumber > 1;
    }

    public onChange($event): void {
        if ($event.target) {
            this.inputFileService
                .validateFiles($event.target.files, this.maxFilesNumber, this.allowedExtensions)
                .subscribe(files => this.upload.emit(files)
                    , error => this.invalidUpload.emit(error));
        }
    }

    public onClickDropzoneInput($event: Event): void {
        $event.stopPropagation();
        const inputFileElement = this.inputFileRef.nativeElement;
        inputFileElement.click();
    }

    public onDragAndDrop($event: File[]): void {
        this.upload.emit($event);
    }

    public onInvalidDragAndDrop($event: string): void {
        this.invalidUpload.emit($event);
    }
}
