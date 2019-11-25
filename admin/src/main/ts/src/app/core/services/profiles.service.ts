import http from 'axios';

export class ProfilesService {

    private static profiles: Array<{name: string, blocked: any}>;

    private constructor() {}

    public static getProfiles(): Promise<Array<{name: string, blocked: any}>> {
        if (!ProfilesService.profiles) {
            return new Promise((resolve, reject) => {
                http.get('/directory/profiles')
                .then(res => {
                    const resArray = res.data as Array<{name: string, blocked: any}>;
                    ProfilesService.profiles = resArray;
                    resolve(ProfilesService.profiles);
                }, err => {
                    resolve([]);
                });
            });
        } else {
            return new Promise(res => res(ProfilesService.profiles));
        }
    }
}
