import http from 'axios'

const SMS_MODULE_KEY: string = 'smsModule'

export class PlateformeInfoService {

    private static infos: Map<string, string | boolean> = new Map<'', ''>()

    public static isSmsModule(): Promise<boolean> {
        if (!PlateformeInfoService.infos.has(SMS_MODULE_KEY)) {
            return http.get('/admin/api/plateforme/module/sms')
                .then(res => {
                    if(res.data) {
                        console.log(res.data.activated)
                        PlateformeInfoService.infos.set(SMS_MODULE_KEY, res.data.activated)
                        return Promise.resolve(res.data.activated)
                    }
                }).catch(err => {
                    return Promise.resolve(false)
                })
        }
        return new Promise(res => res(PlateformeInfoService.infos.get(SMS_MODULE_KEY)))
    }
}
