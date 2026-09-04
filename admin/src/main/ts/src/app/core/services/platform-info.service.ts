import http from 'axios';

const SMS_MODULE_KEY = 'smsModule';
const NO_HEAD_TEACHER_SOURCES_KEY = 'noHeadTeacherSources';
const DEFAULT_NO_HEAD_TEACHER_SOURCES = ['AAF1D'];

export class PlatformInfoService {

    private static infos: Map<string, string | boolean> = new Map<'', ''>();
    private static noHeadTeacherSources: string[];

    public static async getNoHeadTeacherSources(): Promise<string[]> {
        if (!PlatformInfoService.noHeadTeacherSources) {
            try {
                const res = await http.get('/directory/conf/public');
                PlatformInfoService.noHeadTeacherSources =
                    res.data?.[NO_HEAD_TEACHER_SOURCES_KEY] || DEFAULT_NO_HEAD_TEACHER_SOURCES;
            } catch (e) {
                PlatformInfoService.noHeadTeacherSources = DEFAULT_NO_HEAD_TEACHER_SOURCES;
            }
        }
        return PlatformInfoService.noHeadTeacherSources;
    }

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
