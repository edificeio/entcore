import { Injectable } from "@angular/core";
import { Observable } from "rxjs";

@Injectable()
export class InputFileService {
    public validateFiles(files: FileList, maxFilesNumber: number, allowedExtensions: string[]): Observable<File[]> {
        if (!files) {
            return Observable.throw('files is empty');
        }
        if (files.length > maxFilesNumber) {
            return Observable.throw(`Only ${maxFilesNumber} file(s) allowed`);
        }

        let validFiles: File[] = [];
        let invalidFiles: File[] = [];
        for (let i = 0; i < files.length; i++) {
            let filenameSplit = files[i].name.split('.');
            let ext = filenameSplit[filenameSplit.length - 1];
            if (allowedExtensions.lastIndexOf(ext) != -1) {
                validFiles.push(files[i]);
            } else {
                invalidFiles.push(files[i]);
            }
        }

        return Observable.create(observer => {
            if (validFiles.length > 0) {
                observer.next(validFiles);
            }
            if (invalidFiles.length > 0) {
                observer.error(`Extension not allowed. Allowed extensions: ${allowedExtensions}`);
            }
            observer.complete();
        });
    }

    public isSrcExternalUrl(src: string): boolean {
        return src.startsWith("http://") || src.startsWith("https://");
    }

    public isSrcWorkspace(src: string): boolean {
        return src.startsWith('/workspace');
    }
}