import http from 'axios';

const SMS_MODULE_KEY = 'smsModule';

export class PlatformInfoService {

    private static infos: Map<string, string | boolean> = new Map<'', ''>();

    public static isSmsModule(): Promise<string | boolean> {
        if (!PlatformInfoService.infos.has(SMS_MODULE_KEY)) {

            return new Promise((resolve, reject) => {
                http.get('/admin/api/platform/module/sms')
                .then(res => {
                    if (res.data) {
                        PlatformInfoService.infos.set(SMS_MODULE_KEY, res.data.activated);
                        resolve(res.data.activated);
                    }
                }, () => {
                    resolve(false);
                });
            });
        }
        return Promise.resolve(PlatformInfoService.infos.get(SMS_MODULE_KEY));
    }
}
