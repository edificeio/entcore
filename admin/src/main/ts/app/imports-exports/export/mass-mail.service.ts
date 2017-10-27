import http from 'axios'
import qs from 'qs'

export class MassMailService {

    private constructor() {}

    static async getList(structureId): Promise<any> {
        let response;
        try {
            response = await http.get(`directory/structure/${structureId}/massMail/allUsers`);
        }catch(error){
            return error.response.data
        }
        response.data.forEach( user => {
            user.classesStr = user.classes.map( c =>  c.name ).join(" ");
        })
        return response.data
    }

    static async massMailProcess(structureId, type, filters){
        let response;
        try{
            response = await http.get(`directory/structure/${structureId}/massMail/process/${type}`, {
                params: filters,
                paramsSerializer: function(params) {
                return qs.stringify(params, {arrayFormat: 'repeat'})
        },     
        responseType: 'blob'
            });
        }catch(error){
            return error.response.data
        }
        return response.data
    }
}