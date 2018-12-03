import http from 'axios';

const SMS_MODULE_KEY = 'smsModule';

export class PlatformInfoService {

    private static infos: Map<string, string | boolean> = new Map<'', ''>();

    public static isSmsModule(): Promise<boolean> {
        if (!PlatformInfoService.infos.has(SMS_MODULE_KEY)) {
            return http.get('/admin/api/platform/module/sms')
                .then(res => {
                    if (res.data) {
                        PlatformInfoService.infos.set(SMS_MODULE_KEY, res.data.activated);
                        return Promise.resolve(res.data.activated);
                    }
                }).catch(() => {
                    return Promise.resolve(false);
                })
        }
        return new Promise(res => res(PlatformInfoService.infos.get(SMS_MODULE_KEY)));
    }
}
