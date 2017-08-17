import http from 'axios'

export class ProfilesService {

    private static profiles: Array<{name: string, blocked: any}>

    private constructor() {}

    public static getProfiles(): Promise<Array<{name: string, blocked: any}>> {
        if (!ProfilesService.profiles) {
            return http.get('/directory/profiles')
                .then(res => {
                    let resArray = res.data as Array<{name: string, blocked: any}>
                    ProfilesService.profiles = resArray
                    return ProfilesService.profiles
                }).catch(err => {
                    return Promise.resolve([])
                })
        } else {
            return new Promise(res => res(ProfilesService.profiles))
        }
    }
}