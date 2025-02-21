import http from 'axios';
import qs from 'qs';
import {UserModel} from '../../core/store/models/user.model';
import { ImportCSVService } from '../import/import-csv.service';

export class MassMessageService {

    private constructor() {}

    static async massMessagerProcess(structureId, type, filters) {
        let response;
        try {
            response = await http.get(`/directory/structure/${structureId}/massMail/process/${type}`, {
                params: filters,
                paramsSerializer(params) {
                return qs.stringify(params, {arrayFormat: 'repeat'});
        },
        responseType: 'blob'
            });
        } catch (error) {
            return error.response.data;
        }
        return response.data;
    }

    static async getColumnsMapping(importInfos): Promise<any> {
        return this.buildPostFormData(importInfos, 'massmessaging/column/mapping');
    }

    static async populateImportedInfos(mappings, requiredFields): Promise<any> {
        let response;
        try {
            response = await http.post('/directory/massmessaging/validation/populate',
            {"mappings":mappings, "required":requiredFields},
            {headers : { 'Content-Type': 'application/json' }});

        } catch (error) {
            return error.response.data;
        }
        
        return response.data;

    }

    static async sendEmail(importedData): Promise<any> {
        const data = {rows: importedData.rows, headers: importedData.headers, template: importedData.template,
                            messageSubject: importedData.messageSubject};
        let response;
                try {
                    response = await http.post('/directory/massmessaging',data,
                     {headers : { 'Content-Type': 'application/json' }});
                     return response.data;
                } catch (error) {
                    if (error.response && error.response.data) {
                            return error.response.data;
                        } else {
                            return { error: 'An error occurred while sending the email' };
                        }
                }
    }

    static async getDefaultTemplate(): Promise<any> {
        let response;
        try {
            response = await http.get('/directory/structure/massmessaging/template');
        } catch (error) {
            return error.response.data;
        }
        return response.data;
        
    }

    static async getSenderName(): Promise<any> {
        let response;
        try {
            response = await http.get('/directory/massmessaging/senderName');
        } catch (error) {
            return error.response.data;
        }
        return response.data;
        
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
            response = await http.post('/directory/' + apiPath,
                formData, {headers : { 'Content-Type': 'multipart/form-data' }});
        } catch (error) {
            return error.response.data;
        }
        return response.data;
    }
}
