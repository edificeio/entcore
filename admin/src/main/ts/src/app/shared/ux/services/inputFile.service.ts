import {Injectable} from '@angular/core';
import {Observable} from 'rxjs';

@Injectable()
export class InputFileService {
    public validateFiles(files: FileList, maxFilesNumber: number, allowedExtensions: string[]): Observable<File[]> {
        if (!files) {
            return Observable.throw('files is empty');
        }
        if (files.length > maxFilesNumber) {
            return Observable.throw(`Only ${maxFilesNumber} file(s) allowed`);
        }

        const validFiles: File[] = [];
        const invalidFiles: File[] = [];
        for (let i = 0; i < files.length; i++) {
            const filenameSplit = files[i].name.split('.');
            const ext = filenameSplit[filenameSplit.length - 1];
            if (allowedExtensions.lastIndexOf(ext.toLowerCase()) !== -1) {
                validFiles.push(files[i]);
            } else {
                invalidFiles.push(files[i]);
            }
        }

        return new Observable(observer => {
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
        if (src) {
            return src.startsWith('http://') || src.startsWith('https://');
        } else {
            return false;
        }
    }

    public isSrcWorkspace(src: string): boolean {
        if (src) {
            return src.startsWith('/workspace');
        } else {
            return false;
        }
    }
}
