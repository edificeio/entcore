import http from 'axios'
import { FlashMessageModel } from '../../core/store';

export class MessageFlashService {

    private constructor() {}

    static async getMessagesByStructure(structureId: string): Promise<FlashMessageModel[]> {
        let response;
        try {
            response = await http.get(`timeline/flashmsg/listadmin/${structureId}`);
        }catch(error){
            return error.response.data
        }
        return response.data;
    }

    static async deleteMessages(messageIds: string[]): Promise<Object> {
        let params: string = "";
        messageIds.forEach(id => params += `id=${id}&`);
        let response;
        try {
            response = await http.delete(`timeline/flashmsg?${params}`);
        }catch(error){
            return error.response.data
        }
        return response.data;
    }

    static async getLanguages(): Promise<string[]> {
        let response;
        try {
            response = await http.get(`/languages`);
        }catch(error){
            return error.response.data
        }
        return response.data;
    }
}