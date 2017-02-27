import { Session } from './mappings'

import http from 'axios'

export class SessionModel {

    private static session: Session

    public static getSession() : Promise<Session> {
        if(!SessionModel.session) {
            return http.get('/auth/oauth2/userinfo').then((result) => {
                SessionModel.session = result.data as Session
                return SessionModel.session
            }).catch(e => {
                console.error(e)
                return Promise.resolve({})
            })
        } else {
            return new Promise(res => res(SessionModel.session))
        }
    }

}