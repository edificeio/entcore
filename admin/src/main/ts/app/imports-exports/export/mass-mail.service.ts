import http from 'axios'

export class MassMailService {

    private constructor() {}

    static async getList(structureId): Promise<any> {
        let response;
        try {
            response = await http.get('directory/structure/'+structureId+'/massMail/users?a=false')
        }catch(error){
            return error.response.data
        }

        return response.data
    }
}