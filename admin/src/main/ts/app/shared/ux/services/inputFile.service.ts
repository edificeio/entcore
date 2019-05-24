import { Injectable } from "@angular/core";

@Injectable()
export class InputFileService {
    public validateFiles(files: FileList, maxFilesNumber: number, allowedExtensions: string[]): File[] {
        if (!files) {
            throw 'files is empty';
        } 
        
        if (files.length > maxFilesNumber) {
            throw `Only ${maxFilesNumber} file(s) allowed`;
        } else {
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
    
            if (validFiles.length > 0) {
                return validFiles;
            }
    
            if (invalidFiles.length > 0) {
                throw `Extension not allowed. Allowed extensions: ${allowedExtensions}`;
            }
        }
    }

    public isSrcUrl(src: string): boolean {
        return src.startsWith('/workspace') 
            || src.startsWith("http://") 
            || src.startsWith("https://");
    }
}