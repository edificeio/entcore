import http from 'axios'

export class ImportCSVService {

    private constructor() {}

    static async getColumnsMapping(importInfos): Promise<any> {
        return this.buildPostFormData(importInfos, 'column/mapping');
    }

    static async getClassesMapping(importInfos, columnsMapping): Promise<any> {
        let dataToPost = Object.assign({},importInfos);
        dataToPost['columnsMapping'] = JSON.stringify(columnsMapping);
        return this.buildPostFormData(dataToPost, 'classes/mapping');
    }

    static async validate(importInfos, columnsMapping, classesMapping): Promise<any> {
        let importId = importInfos.importId != null ? '/' + importInfos.importId : '';
        let dataToPost = Object.assign({},importInfos);
        dataToPost['columnsMapping'] = JSON.stringify(columnsMapping);
        dataToPost['classesMapping'] = JSON.stringify(classesMapping);
        return this.buildPostFormData(dataToPost, 'validate' + importId);
    }

    private static async buildPostFormData(importInfos, apiPath): Promise<any> {
        let formData = new FormData();
        for(let key in importInfos){
            formData.append(key, importInfos[key]);
        }
        let response;
        try {
            response = await http.post('directory/wizard/' + apiPath, 
                formData, {'headers' : { 'Content-Type': 'multipart/form-data' }});
        } catch(error) {
            return error.response.data;
        }
        return response.data;
    }

    static async updateReport(action, importId, profile, data): Promise<any> {
        let response;
        let path = ['directory/wizard/update', importId, profile].join('/');
        try {
            response = await http.put(path, data);
        } catch(error) {
            return error.response.data;
        }
        return response.data;
    }

    static async import(importId): Promise<any> {
        let response;
        try {
            response = await http.put('directory/wizard/import/' + importId);
        } catch(error) {
            return error.response.data;
        }
        return response.data 
    }

}