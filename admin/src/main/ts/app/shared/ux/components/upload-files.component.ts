import { Component, Output, EventEmitter, Input, ViewChild, ElementRef, OnInit } from "@angular/core";
import { InputFileService } from "../services/inputFile.service";

@Component({
    selector: 'upload-files',
    template: `
        <div class="upload-files-dropzone" 
            dragAndDropFiles 
            [allowedExtensions]="allowedExtensions" 
            [maxFilesNumber]="maxFilesNumber"
            (dragAndDrop)="onDragAndDrop($event)" 
            (invalidDragAndDrop)="onInvalidDragAndDrop($event)">
            <div *ngIf="fileSrc" class="upload-files-dropzone-image">
                <img *ngIf="inputFileService.isSrcUrl(fileSrc) else isFileIcon" 
                    src="{{ fileSrc }}" 
                    class="upload-files-dropzone-image__image">
                <ng-template #isFileIcon>
                    <i class="upload-files-dropzone-image__icon {{ fileSrc }}"></i>
                </ng-template>
            </div>

            <div class="upload-files-dropzone-input">
                <button class="confirm upload-files-dropzone-input__button" 
                    (click)="onClickDropzoneInput($event)">
                    <s5l>ux.upload-files.button.label</s5l>
                </button>
                <i class="upload-files-dropzone-input__icon fa fa-cloud-upload fa-2x">
                </i>
                <span class="upload-files-dropzone-input__help">
                    <s5l>ux.upload-files.button.help</s5l>
                </span>
                <input *ngIf="multiple === true else singleFileInput"
                    class="upload-files-dropzone-input__input" 
                    type="file" 
                    (change)="onChange($event)"
                    multiple
                    #inputFileRef>
                <ng-template #singleFileInput>
                    <input class="upload-files-dropzone-input__input" 
                        type="file" 
                        (change)="onChange($event)"
                        #inputFileRef>
                </ng-template>
            </div>
        </div>
    `,
    styles: [`
        .upload-files-dropzone {
            display: flex;
            align-items: center;
            justify-content: center;
            min-height: 150px;
            margin: 20px 20px;
            padding: 20px 10px;
            background-color: #fafafa;
            border: 2px dashed #ddd;
            border-radius: 5px;
        }
        .upload-files-dropzone-image {
            
        }
        .upload-files-dropzone-image__image {
            height: 130px;
            width: 130px;
            margin: 20px 20px;
        }
        .upload-files-dropzone-image__icon {
            font-size: 7em;
            margin: 20px 20px;
        }
        .upload-files-dropzone-input {
            display: flex;
            align-items: center;
            background-color: #fff;
            border-radius: 5px;
            box-shadow: 1px 1px 5px rgba(0,0,0,0.3);
            padding: 10px;
            margin: 0px 40px 0px 20px;
        }
        .upload-files-dropzone-input__input {
            display: none;
        }
        .upload-files-dropzone-input__button {
            
        }
        .upload-files-dropzone-input__icon {
            margin-left: 10px;
        }
        .upload-files-dropzone-input__help {
            margin-left: 10px;
            font-style: italic;
        }
    `]
})
export class UploadFilesComponent implements OnInit {
    @Input()
    fileSrc: string;
    @Input()
    allowedExtensions: Array<string>;
    @Input()
    maxFilesNumber: number;
    
    @Output()
    upload: EventEmitter<File[]> = new EventEmitter();
    @Output()
    invalidUpload: EventEmitter<string> = new EventEmitter();

    @ViewChild('inputFileRef')
    inputFileRef: ElementRef;

    public multiple: boolean;

    constructor(private inputFileService: InputFileService) {
    }

    public ngOnInit(): void {
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
        let inputFileElement = this.inputFileRef.nativeElement;
        inputFileElement.click();
    }

    public onDragAndDrop($event: File[]): void {
        this.upload.emit($event);
    }

    public onInvalidDragAndDrop($event: string): void {
        this.invalidUpload.emit($event);
    }
}