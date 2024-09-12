import http from 'axios';
import { FlashMessageModel } from 'src/app/core/store/models/flashmessage.model';
import { SessionModel } from 'src/app/core/store/models/session.model';
import { StructureModel } from 'src/app/core/store/models/structure.model';

export class MessageFlashService {

    private constructor() {}

    static async getMessagesByStructure(structureId: string): Promise<FlashMessageModel[]> {
        let response;
        try {
            response = await http.get(`/timeline/flashmsg/listadmin/${structureId}`);
        } catch (error) {
            return error.response.data;
        }
        return response.data;
    }

    static async getSubStructuresByMessageId(messageId: string): Promise<string[]> {
        let response;
        try {
            response = await http.get(`/timeline/flashmsg/${messageId}/substructures`);
        } catch (error) {
            return error.response.data;
        }
        return response.data;
    }

    static async deleteMessages(messageIds: string[], structureId: string): Promise<Object> {
        let params = '';
        messageIds.forEach(id => params += `id=${id}&`);
        let response;
        try {
            response = await http.delete(`/timeline/flashmsg/structure/${structureId}?${params}`);
        } catch (error) {
            return error.response.data;
        }
        return response.data;
    }

    static async getLanguages(): Promise<string[]> {
        let response;
        try {
            response = await http.get(`/languages`);
        } catch (error) {
            return error.response.data;
        }
        return response.data;
    }

    static async createMessage(message: FlashMessageModel) {
        let response;
        try {
            await http.post(`/timeline/flashmsg/structure/${message.structureId}`,
            {
                title: message.title,
                contents: message.contents,
                startDate: this.startOfDay(message.startDate),
                endDate: this.endOfDay(message.endDate),
                profiles: message.profiles,
                color: message.color,
                customColor: message.customColor,
                structureId: message.structureId,
                signature: message.signature,
                signatureColor: message.signatureColor
            }).then( async (data) => {
                response = await http.post(`/timeline/flashmsg/structure/${message.structureId}/${data.data.id}/substructures`,
                {
                    subStructures: message.subStructures
                });
            });
        } catch (error) {
            return error.res.data;
        }
        return response.data;
    }

    static async editMessage(message: FlashMessageModel) {
        let response1, response2, res;
        try {
            response1 = await http.put(`/timeline/flashmsg/structure/${message.structureId}/${message.id}`,
            {
                title: message.title,
                contents: message.contents,
                startDate: this.startOfDay(message.startDate),
                endDate: this.endOfDay(message.endDate),
                profiles: message.profiles,
                color: message.color,
                customColor: message.customColor,
                structureId: message.structureId,
                signature: message.signature,
                signatureColor: message.signatureColor
            });
            response2 = await http.post(`/timeline/flashmsg/structure/${message.structureId}/${message.id}/substructures`,
            {
                subStructures: message.subStructures
            });
            res = Promise.all([response1, response2]);
        } catch (error) {
            return error.res.data;
        }
        return res.data;
    }

    static async sendNotifications(message: FlashMessageModel, structure: StructureModel, lang: string,
                                   mailNotification: boolean, pushNotification: boolean) {
        let response;
        try {
            await structure.users.sync().then(async (users) => {
                const recipients: string[] = users.data.filter(user =>
                    message.profiles.some(profile => {
                        if (profile == 'AdminLocal'
                            && user.functions.findIndex(func => func[0] == 'ADMIN_LOCAL' && func[1].includes(message.structureId)) != -1) {
                                return true;
                        } else {
                            if (user.type == profile) {
                                return true;
                            }
                        }
                        return false;
                    })
                ).map(user => user.id);
                response = await http.post(`/timeline/flashmsg/notify`,
                    {
                        recipientIds: recipients,
                        content: message.contents[lang],
                        mailNotification,
                        pushNotification
                    });
            });
        } catch (error) {
            return error.res.data;
        }
        return response.data;
    }

    private static startOfDay(startDate: string): string {
        const date: Date = new Date(startDate);
        date.setHours(0, 0, 0, 0);
        return date.toISOString();
    }

    private static endOfDay(endDate: string): string {
        const date: Date = new Date(endDate);
        date.setHours(23, 59, 59, 999);
        return date.toISOString();
    }

    static async getTheme() {
        try {
            const theme = await SessionModel.getTheme();
            return theme.skin + 'theme.css';
        } catch (error) {
            return error.response.data;
        }
    }
}
