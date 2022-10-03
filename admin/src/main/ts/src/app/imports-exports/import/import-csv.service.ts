import http from 'axios';
import * as chardet  from 'chardet';

export class ImportCSVService {

    private constructor() {}

    static async getColumnsMapping(importInfos): Promise<any> {
        return this.buildPostFormData(importInfos, 'column/mapping');
    }

    static async getClassesMapping(importInfos, columnsMapping): Promise<any> {
        const dataToPost = Object.assign({}, importInfos);
        dataToPost.columnsMapping = JSON.stringify(columnsMapping);
        return this.buildPostFormData(dataToPost, 'classes/mapping');
    }

    static async validate(importInfos, columnsMapping, classesMapping): Promise<any> {
        const importId = importInfos.importId != null ? '/' + importInfos.importId : '';
        const dataToPost = Object.assign({}, importInfos);
        dataToPost.columnsMapping = JSON.stringify(columnsMapping);
        dataToPost.classesMapping = JSON.stringify(classesMapping);
        return this.buildPostFormData(dataToPost, 'validate' + importId);
    }

    private static async buildPostFormData(importInfos, apiPath): Promise<any> {
        const formData = new FormData();
        try {
            for (const key in importInfos) {
                const value = importInfos[key];
                if(value instanceof File){
                    const decoded = await ImportCSVService.fixEncoding(value);
                    formData.append(key, decoded, value.name);
                }else{
                    formData.append(key, value);
                }
            }
        } catch (error) {
            return {error};
        }
        let response;
        try {
            response = await http.post('/directory/wizard/' + apiPath,
                formData, {headers : { 'Content-Type': 'multipart/form-data' }});
        } catch (error) {
            return error.response.data;
        }
        return response.data;
    }

    static async updateReport(action, importId, profile, data): Promise<any> {
        let response;
        const path = ['/directory/wizard/update', importId, profile].join('/');
        try {
            response = await http[action](path, data);
        } catch (error) {
            return error.response.data;
        }
        return response.data;
    }

    static async deleteLineReport(importId, profile, line): Promise<any> {
        let response;
        try {
            response = await http.delete(['/directory/wizard/update', importId, profile, line].join('/'));
        } catch (error) {
            return error.response.data;
        }
        return response.data;
    }

    static async import(importId): Promise<any> {
        let response;
        try {
            response = await http.put('/directory/wizard/import/' + importId);
        } catch (error) {
            if (error.response) {
                return error.response.data;
            }
            throw error;
        }
        return response.data;
    }

    static fixEncoding(file:File):Promise<Blob>{
        return new Promise((resolve, reject)=>{
            const reader = new FileReader();
            reader.onload = function() {
                try{
                    const arrayBuffer = this.result as ArrayBuffer;
                    const array = new Uint8Array(arrayBuffer)
                    const result = ImportCSVService.decodeFile(array);
                    ImportCSVService.checkSeparator(result);
                    const utf8 = ImportCSVService.encodeUtf8(result);
                    const blob = new Blob([utf8.buffer],{
                        type: file.type
                    })
                    resolve(blob);
                }catch(e){
                    reject(e);
                }
            }
            reader.onerror = function(e){
                reject(this.error);
            }
            reader.readAsArrayBuffer(file);
        })
    }

    static encodeUtf8(data:string){
        const encoder = new TextEncoder();
        return encoder.encode(data);
    }

    static checkSeparator(file:string){
        const lines = file.split("\n");
        const firstLine = lines[0] || "";
        if(firstLine.indexOf(";")==-1){
            throw "import.error.file.separator";
        }
    }

    static decodeFile(data:Uint8Array):string{
        let detect = chardet.detect(data);
        if(detect == "windows-1252"){
            //fix macintosh (detected as window-1252)
            if(ImportCSVService.isMacintosh(data)){
                detect = "macintosh";
            }
        }
        const input = new TextDecoder(detect, { NONSTANDARD_allowLegacyEncoding: true, fatal: true, ignoreBOM: true} as any).decode(data);
        return input;
    }

    static isMacintosh(buf) {
        //ascii => https://www.ascii-code.com/
        //macinstosh => 135 to 144 https://string-functions.com/encodingtable.aspx?encoding=65001&decoding=10000
        //const list = ["\x87","\x88","\x89","\x8a","\x8b","\x8c","\x8d","\x8e","\x8f","\x90"];
        for(var i = 0 ; i < buf.length; i ++){
            const code = buf[i];
            if(135 <= code && code <145){
                return true;
            }
        }
        return false;
    }

}
